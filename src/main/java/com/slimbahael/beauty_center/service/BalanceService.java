package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.BalanceTransactionRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final UserRepository userRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;
    private final StripeService stripeService;



    public BigDecimal getUserBalance(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
    }

    public List<BalanceTransaction> getUserTransactionHistory(String userId) {
        return balanceTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public BalanceTransaction creditBalanceFromIntent(String userId, String paymentIntentId) {
        // 1) fetch the intent
        PaymentIntent intent = stripeService.getPaymentIntent(paymentIntentId);

        // ── Insert your debug log here ──
        Map<String,String> md = intent.getMetadata();
        log.info ("Stripe metadata for intent {}: {}", paymentIntentId, intent.getMetadata());
        // ────────────────────────────────

        // 2) verify it actually succeeded
        if (!"succeeded".equals(intent.getStatus())) {
            throw new BadRequestException(
                    "PaymentIntent " + paymentIntentId + " not succeeded (status=" + intent.getStatus() + ")");
        }

        // 3) verify metadata.user_id matches
        String metaUserId = intent.getMetadata().get("user_id");
        if (metaUserId == null) {
            log.warn("PaymentIntent {} has no user_id metadata. This might be an old PaymentIntent created before the metadata fix.", paymentIntentId);
            // For backward compatibility, we'll allow this if the payment succeeded
            // but log a warning for monitoring
        } else if (!metaUserId.equals(userId)) {
            throw new BadRequestException("This PaymentIntent doesn't belong to user " + userId);
        }

        // 4) idempotency: make sure we haven't already applied this intent
        if (balanceTransactionRepository.existsByOrderId(paymentIntentId)) {
            throw new BadRequestException("PaymentIntent " + paymentIntentId + " has already been applied");
        }

        // 5) compute the amount in euros
        BigDecimal amount =
                BigDecimal.valueOf(intent.getAmountReceived())
                        .movePointLeft(2);  // cents → euros

        // 6) finally credit the user
        return creditBalance(
                userId,
                amount,
                "Top-up via Stripe",
                "CREDIT",
                paymentIntentId
        );
    }


    @Transactional
    public BalanceTransaction creditBalance(String userId, BigDecimal amount, String description,
                                            String transactionType, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Credit amount must be positive");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        BigDecimal balanceBefore = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(amount);

        // Update user balance
        user.setBalance(balanceAfter);
        user.setLastBalanceUpdate(new Date());
        userRepository.save(user);

        // Create transaction record
        BalanceTransaction transaction = BalanceTransaction.builder()
                .userId(userId)
                .transactionType(transactionType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .status("COMPLETED")
                .orderId(referenceId)
                .createdAt(new Date())
                .completedAt(new Date())
                .build();

        return balanceTransactionRepository.save(transaction);
    }

    @Transactional
    public BalanceTransaction debitBalance(String userId, BigDecimal amount, String description,
                                           String transactionType, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debit amount must be positive");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        BigDecimal balanceBefore = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;

        if (balanceBefore.compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance. Available: " + balanceBefore + ", Required: " + amount);
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // Update user balance
        user.setBalance(balanceAfter);
        user.setLastBalanceUpdate(new Date());
        userRepository.save(user);

        // Create transaction record
        BalanceTransaction transaction = BalanceTransaction.builder()
                .userId(userId)
                .transactionType(transactionType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .status("COMPLETED")
                .orderId(referenceId)
                .createdAt(new Date())
                .completedAt(new Date())
                .build();

        return balanceTransactionRepository.save(transaction);
    }

    @Transactional
    public BalanceTransaction refundToBalance(String userId, BigDecimal amount, String description, String orderId) {
        return creditBalance(userId, amount, description, "REFUND", orderId);
    }

    @Transactional
    public BalanceTransaction processBalancePayment(String userId, BigDecimal amount, String description, String orderId) {
        return debitBalance(userId, amount, description, "DEBIT", orderId);
    }

    public boolean hasInsufficientBalance(String userId, BigDecimal requiredAmount) {
        BigDecimal currentBalance = getUserBalance(userId);
        return currentBalance.compareTo(requiredAmount) < 0;
    }

    @Transactional
    public BalanceTransaction adminAdjustBalance(String userId, BigDecimal amount, String description, String adminId) {
        String transactionType = amount.compareTo(BigDecimal.ZERO) > 0 ? "CREDIT" : "DEBIT";

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            BalanceTransaction transaction = creditBalance(userId, amount, description, transactionType, null);
            transaction.setAdminId(adminId);
            return balanceTransactionRepository.save(transaction);
        } else {
            BalanceTransaction transaction = debitBalance(userId, amount.abs(), description, transactionType, null);
            transaction.setAdminId(adminId);
            return balanceTransactionRepository.save(transaction);
        }
    }

    public BalanceTransaction addTransaction(
            User user,
            BigDecimal amount,
            String type, // "CREDIT", "DEBIT", etc.
            String description
    ) {
        BalanceTransaction tx = BalanceTransaction.builder()
                .userId(user.getId())
                .transactionType(type)
                .amount(amount)
                .balanceBefore(user.getBalance().subtract(amount)) // le solde avant ajout
                .balanceAfter(user.getBalance()) // le solde après ajout
                .description(description)
                .status("COMPLETED")
                .createdAt(new Date())
                .completedAt(new Date())
                .build();

        return balanceTransactionRepository.save(tx);
    }



}