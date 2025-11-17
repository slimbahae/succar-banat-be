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

    // BALANCE FUNCTIONALITY DISABLED - Balance system temporarily unavailable
    @GetMapping("/customer/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getCustomerBalance(Authentication authentication) {
        throw new BadRequestException("Le système de solde est temporairement désactivé.");
    }

    @GetMapping("/customer/balance/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BalanceTransaction>> getCustomerTransactionHistory(Authentication authentication) {
        throw new BadRequestException("Le système de solde est temporairement désactivé.");
    }

    @PostMapping("/customer/balance/create-payment-intent")
    public PaymentIntentResponse createBalancePaymentIntent(
            @RequestBody PaymentIntentRequest request,
            Principal principal
    ) {
        throw new BadRequestException("Le système de solde est temporairement désactivé.");
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
        throw new BadRequestException("Le système de solde est temporairement désactivé.");
    }

    @GetMapping("/admin/users/{userId}/balance/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BalanceTransaction>> getUserTransactionHistory(@PathVariable String userId) {
        List<BalanceTransaction> transactions = balanceService.getUserTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }



}