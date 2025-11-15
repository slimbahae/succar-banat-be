package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.PaymentIntentRequest;
import com.slimbahael.beauty_center.dto.PaymentIntentResponse;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.service.BalanceService;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.dto.BalanceAdjustmentRequest;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.service.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    @GetMapping("/customer/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getCustomerBalance(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BigDecimal balance = balanceService.getUserBalance(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("balance", balance);
        result.put("formatted", String.format("€%.2f", balance));
        result.put("lastUpdated", user.getLastBalanceUpdate()); // may be null, which is okay with HashMap

        return ResponseEntity.ok(result);
    }

    @GetMapping("/customer/balance/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BalanceTransaction>> getCustomerTransactionHistory(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<BalanceTransaction> transactions = balanceService.getUserTransactionHistory(user.getId());
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/customer/balance/create-payment-intent")
    public PaymentIntentResponse createBalancePaymentIntent(
            @RequestBody PaymentIntentRequest request,
            Principal principal
    ) {
        // 1) Look up the user
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // 2) **Populate the metadata fields** before handing off to StripeService
        request.setUserId(user.getId());
        request.setCustomerEmail(user.getEmail());

        // 3) Now the metadata map in StripeService.createPaymentIntent() will contain them
        return stripeService.createPaymentIntent(request);
    }

    @GetMapping("/admin/users/{userId}/balance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserBalance(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BigDecimal balance = balanceService.getUserBalance(userId);
        List<BalanceTransaction> recentTransactions = balanceService.getUserTransactionHistory(userId);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "userName", user.getFirstName() + " " + user.getLastName(),
                "balance", balance,
                "formatted", String.format("€%.2f", balance),
                "lastUpdated", user.getLastBalanceUpdate(),
                "recentTransactions", recentTransactions.stream().limit(5).toList()
        ));
    }

    @PostMapping("/admin/users/{userId}/balance/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BalanceTransaction> adjustUserBalance(
            @PathVariable String userId,
            @Valid @RequestBody BalanceAdjustmentRequest request,
            Authentication authentication) {

        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        BalanceTransaction transaction = balanceService.adminAdjustBalance(
                userId,
                request.getAmount(),
                request.getDescription(),
                admin.getId()
        );

        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/customer/balance/top-up")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BalanceTransaction> topUpBalance(
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        // 1) pull the PaymentIntent ID from the JSON body
        String paymentIntentId = request.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new BadRequestException("paymentIntentId is required");
        }

        // 2) resolve current user
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3) delegate to your service (which does fetch + verify + idempotency + credit)
        BalanceTransaction tx = balanceService.creditBalanceFromIntent(user.getId(), paymentIntentId);

        // 4) return the recorded transaction
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/admin/users/{userId}/balance/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BalanceTransaction>> getUserTransactionHistory(@PathVariable String userId) {
        List<BalanceTransaction> transactions = balanceService.getUserTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }



}