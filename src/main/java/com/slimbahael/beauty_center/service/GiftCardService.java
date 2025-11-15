package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.model.GiftCard;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.GiftCardRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.dto.GiftCardPurchaseRequest;
import com.slimbahael.beauty_center.dto.GiftCardRedemptionRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Calendar;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class GiftCardService {

    private final GiftCardRepository giftCardRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;
    private final EmailService emailService;
    private final StripeService stripeService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int CODE_LENGTH = 32;
    private static final int MAX_REDEMPTION_ATTEMPTS = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 10;
    private static final int EXPIRATION_MONTHS = 6;

    @Transactional
    public GiftCard createGiftCard(GiftCardPurchaseRequest request) {
        // 1. Validate payment was successful
        if (request.getPaymentIntentId() == null || request.getPaymentIntentId().trim().isEmpty()) {
            throw new BadRequestException("Payment intent ID is required");
        }

        if (!stripeService.isPaymentSucceeded(request.getPaymentIntentId())) {
            String paymentStatus = stripeService.getPaymentStatus(request.getPaymentIntentId());
            throw new BadRequestException("Payment not confirmed. Current status: " + paymentStatus + ". Please complete payment first.");
        }

        // 2. Check if gift card was already created for this payment
        List<GiftCard> existingCards = giftCardRepository.findByPaymentIntentId(request.getPaymentIntentId());
        if (!existingCards.isEmpty()) {
            log.warn("Attempt to create duplicate gift card for payment intent: {}", request.getPaymentIntentId());
            throw new BadRequestException("Gift card already created for this payment");
        }

        // 3. Validate payment amount matches request amount
        try {
            com.stripe.model.PaymentIntent paymentIntent = stripeService.getPaymentIntent(request.getPaymentIntentId());
            BigDecimal paidAmount = BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)); // Convert from cents

            if (paidAmount.compareTo(request.getAmount()) != 0) {
                throw new BadRequestException("Payment amount (" + paidAmount + "€) does not match gift card amount (" + request.getAmount() + "€)");
            }
        } catch (Exception e) {
            log.error("Error validating payment amount for payment intent: {}", request.getPaymentIntentId(), e);
            throw new BadRequestException("Unable to validate payment amount");
        }

        // 4. Generate secure code
        String rawCode = generateSecureCode();
        String codeHash = passwordEncoder.encode(rawCode);

        // 5. Generate verification token for admin use
        String verificationToken = generateVerificationToken();

        // 6. Calculate expiration date (6 months from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, EXPIRATION_MONTHS);
        Date expirationDate = calendar.getTime();

        // 7. Create gift card
        GiftCard giftCard = GiftCard.builder()
                .codeHash(codeHash)
                .type(request.getType())
                .amount(request.getAmount())
                .status("ACTIVE")
                .purchaserEmail(request.getPurchaserEmail())
                .purchaserName(request.getPurchaserName())
                .recipientEmail(request.getRecipientEmail())
                .recipientName(request.getRecipientName())
                .message(request.getMessage())
                .expirationDate(expirationDate)
                .verificationToken(verificationToken)
                .paymentIntentId(request.getPaymentIntentId())
                .build();

        GiftCard savedGiftCard = giftCardRepository.save(giftCard);

        // 8. Record purchase transaction if purchaser is a user
        recordPurchaseTransaction(request, savedGiftCard.getId());

        // 9. Send emails
        try {
            emailService.sendGiftCardPurchaseConfirmation(request.getPurchaserEmail(), savedGiftCard, rawCode);
            emailService.sendGiftCardReceived(request.getRecipientEmail(), savedGiftCard, rawCode);
            if ("SERVICE".equals(savedGiftCard.getType())) {
                emailService.sendAdminServiceGiftCardNotification(savedGiftCard);
            }
        } catch (Exception e) {
            log.error("Failed to send gift card emails for: {}", savedGiftCard.getId(), e);
            // Don't fail the transaction for email issues
        }

        log.info("Gift card created successfully: {} for recipient: {} with payment intent: {}",
                savedGiftCard.getId(), request.getRecipientEmail(), request.getPaymentIntentId());

        return savedGiftCard;
    }

    @Transactional
    public BalanceTransaction redeemGiftCard(String code, String userId, String ipAddress) {
        // Find gift card by code
        Optional<GiftCard> giftCardOpt = findGiftCardByCode(code);

        if (giftCardOpt.isEmpty()) {
            log.warn("Gift card redemption failed - invalid code from IP: {}", ipAddress);
            throw new BadRequestException("Code cadeau invalide");
        }

        GiftCard giftCard = giftCardOpt.get();

        // Validate redemption
        validateRedemption(giftCard, userId, ipAddress);

        // Only balance type can be redeemed directly
        if (!"BALANCE".equals(giftCard.getType())) {
            throw new BadRequestException("Ce type de carte cadeau ne peut pas être utilisé pour recharger le solde");
        }

        // Update gift card status
        giftCard.setStatus("REDEEMED");
        giftCard.setRedeemedAt(new Date());
        giftCard.setRedeemedByUserId(userId);
        giftCard.setLastRedemptionAttempt(new Date());
        giftCard.setLastRedemptionIp(ipAddress);
        giftCardRepository.save(giftCard);

        // Add balance to user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BalanceTransaction transaction = balanceService.creditBalance(
                userId,
                giftCard.getAmount(),
                "Utilisation carte cadeau - " + giftCard.getId().substring(0, 8),
                "GIFT_CARD_REDEEM",
                giftCard.getId()
        );

        // Send confirmation emails
        try {
            emailService.sendGiftCardRedemptionConfirmation(user.getEmail(), giftCard);
            emailService.sendGiftCardRedeemedNotification(giftCard.getPurchaserEmail(), giftCard);
        } catch (Exception e) {
            log.error("Failed to send redemption emails for gift card: {}", giftCard.getId(), e);
        }

        log.info("Gift card redeemed: {} by user: {}", giftCard.getId(), userId);

        return transaction;
    }

    public GiftCard verifyGiftCardForAdmin(String verificationToken) {
        GiftCard giftCard = giftCardRepository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new ResourceNotFoundException("Token de vérification invalide"));

        // Update verification attempts
        giftCard.setVerificationAttempts(giftCard.getVerificationAttempts() + 1);
        giftCard.setLastVerificationAttempt(new Date());

        if (giftCard.getVerificationAttempts() > MAX_VERIFICATION_ATTEMPTS) {
            giftCard.setIsLocked(true);
            giftCard.setLockedAt(new Date());
            giftCard.setLockedReason("Trop de tentatives de vérification");
            log.warn("Gift card locked due to excessive verification attempts: {}", giftCard.getId());
        }

        giftCardRepository.save(giftCard);

        return giftCard;
    }

    @Transactional
    public void markServiceGiftCardAsUsed(String giftCardId, String adminId) {
        GiftCard giftCard = giftCardRepository.findById(giftCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Carte cadeau non trouvée"));

        if (!"SERVICE".equals(giftCard.getType())) {
            throw new BadRequestException("Cette carte cadeau n'est pas de type service");
        }

        if (!"ACTIVE".equals(giftCard.getStatus())) {
            throw new BadRequestException("Cette carte cadeau n'est pas active");
        }

        giftCard.setStatus("REDEEMED");
        giftCard.setRedeemedAt(new Date());
        giftCard.setRedeemedByUserId(adminId);
        giftCardRepository.save(giftCard);

        // Send confirmation emails
        try {
            emailService.sendServiceGiftCardUsedConfirmation(giftCard.getRecipientEmail(), giftCard);
            emailService.sendServiceGiftCardUsedNotification(giftCard.getPurchaserEmail(), giftCard);
        } catch (Exception e) {
            log.error("Failed to send service gift card used emails: {}", giftCardId, e);
        }

        log.info("Service gift card marked as used: {} by admin: {}", giftCardId, adminId);
    }

    public List<GiftCard> getUserPurchasedGiftCards(String email) {
        return giftCardRepository.findByPurchaserEmailOrderByCreatedAtDesc(email);
    }

    public List<GiftCard> getUserReceivedGiftCards(String email) {
        return giftCardRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    public GiftCard getGiftCardByPaymentIntent(String paymentIntentId) {
        List<GiftCard> giftCards = giftCardRepository.findByPaymentIntentId(paymentIntentId);
        if (giftCards.isEmpty()) {
            throw new ResourceNotFoundException("No gift card found for payment intent: " + paymentIntentId);
        }
        return giftCards.get(0); // Should only be one per payment intent
    }

    @Transactional
    public void cancelGiftCardForFailedPayment(String paymentIntentId) {
        List<GiftCard> giftCards = giftCardRepository.findByPaymentIntentId(paymentIntentId);
        for (GiftCard giftCard : giftCards) {
            if ("ACTIVE".equals(giftCard.getStatus())) {
                giftCard.setStatus("CANCELLED");
                giftCard.setLockedReason("Payment failed or cancelled");
                giftCard.setIsLocked(true);
                giftCard.setLockedAt(new Date());
                giftCardRepository.save(giftCard);
                log.info("Gift card cancelled due to payment failure: {}", giftCard.getId());
            }
        }
    }

    @Transactional
    public void expireGiftCards() {
        List<GiftCard> expiredCards = giftCardRepository.findExpiredActiveGiftCards(new Date());

        for (GiftCard card : expiredCards) {
            card.setStatus("EXPIRED");
            giftCardRepository.save(card);

            // Send expiration notification
            try {
                emailService.sendGiftCardExpiredNotification(card.getRecipientEmail(), card);
                emailService.sendGiftCardExpiredNotification(card.getPurchaserEmail(), card);
            } catch (Exception e) {
                log.error("Failed to send expiration emails for gift card: {}", card.getId(), e);
            }
        }

        log.info("Expired {} gift cards", expiredCards.size());
    }

    private String generateSecureCode() {
        byte[] bytes = new byte[CODE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Optional<GiftCard> findGiftCardByCode(String code) {
        List<GiftCard> activeCards = giftCardRepository.findActiveUnlockedGiftCards();

        for (GiftCard card : activeCards) {
            if (passwordEncoder.matches(code, card.getCodeHash())) {
                return Optional.of(card);
            }
        }

        return Optional.empty();
    }

    private void validateRedemption(GiftCard giftCard, String userId, String ipAddress) {
        // Check if card is active
        if (!"ACTIVE".equals(giftCard.getStatus())) {
            throw new BadRequestException("Cette carte cadeau n'est plus active");
        }

        // Check if card is locked
        if (giftCard.getIsLocked()) {
            throw new BadRequestException("Cette carte cadeau est bloquée");
        }

        // Check expiration
        if (giftCard.getExpirationDate().before(new Date())) {
            giftCard.setStatus("EXPIRED");
            giftCardRepository.save(giftCard);
            throw new BadRequestException("Cette carte cadeau a expiré");
        }

        // Update redemption attempts
        giftCard.setRedemptionAttempts(giftCard.getRedemptionAttempts() + 1);
        giftCard.setLastRedemptionAttempt(new Date());
        giftCard.setLastRedemptionIp(ipAddress);

        // Check for too many attempts
        if (giftCard.getRedemptionAttempts() > MAX_REDEMPTION_ATTEMPTS) {
            giftCard.setIsLocked(true);
            giftCard.setLockedAt(new Date());
            giftCard.setLockedReason("Trop de tentatives d'utilisation");
            giftCardRepository.save(giftCard);
            throw new BadRequestException("Cette carte cadeau est bloquée pour sécurité");
        }
    }

    private void recordPurchaseTransaction(GiftCardPurchaseRequest request, String giftCardId) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getPurchaserEmail());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                balanceService.addTransaction(
                        user,
                        request.getAmount(),
                        "GIFT_CARD_PURCHASE",
                        "Achat carte cadeau - " + giftCardId.substring(0, 8)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to record purchase transaction for gift card: {}", giftCardId, e);
        }
    }
}