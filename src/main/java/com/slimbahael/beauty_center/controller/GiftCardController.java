package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.GiftCardPurchaseRequest;
import com.slimbahael.beauty_center.dto.GiftCardRedemptionRequest;
import com.slimbahael.beauty_center.dto.PaymentIntentRequest;
import com.slimbahael.beauty_center.dto.PaymentIntentResponse;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.GiftCard;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.service.GiftCardService;
import com.slimbahael.beauty_center.service.StripeService;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GiftCardController {

    private final GiftCardService giftCardService;
    private final StripeService stripeService;
    private final UserRepository userRepository;

    // GIFT CARD PURCHASE/REDEMPTION DISABLED - Purchases and balance redemption temporarily unavailable

    @PostMapping("/customer/gift-cards/create-payment-intent")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentIntentResponse> createGiftCardPaymentIntent(
            @Valid @RequestBody GiftCardPurchaseRequest request,
            Authentication authentication) {
        throw new BadRequestException("Les achats de cartes cadeaux sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @PostMapping("/customer/gift-cards/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GiftCard> purchaseGiftCard(
            @Valid @RequestBody GiftCardPurchaseRequest request,
            Authentication authentication) {
        throw new BadRequestException("Les achats de cartes cadeaux sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @GetMapping("/customer/gift-cards/payment-status/{paymentIntentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getGiftCardPaymentStatus(@PathVariable String paymentIntentId) {
        throw new BadRequestException("Les achats de cartes cadeaux sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @PostMapping("/customer/gift-cards/redeem")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BalanceTransaction> redeemGiftCard(
            @Valid @RequestBody GiftCardRedemptionRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddressHeader,
            @RequestHeader(value = "X-Real-IP", required = false) String realIpHeader,
            @RequestHeader(value = "Remote_Addr", required = false) String remoteAddrHeader) {
        throw new BadRequestException("Le système de solde est temporairement désactivé. Les cartes cadeaux ne peuvent pas être échangées contre du solde.");
    }

    @GetMapping("/customer/gift-cards/purchased")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<GiftCard>> getPurchasedGiftCards(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(
                giftCardService.getUserPurchasedGiftCards(user.getEmail()));
    }

    @GetMapping("/customer/gift-cards/received")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<GiftCard>> getReceivedGiftCards(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(
                giftCardService.getUserReceivedGiftCards(user.getEmail()));
    }

    @PostMapping("/customer/gift-cards/cancel-payment/{paymentIntentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> cancelGiftCardPayment(
            @PathVariable String paymentIntentId) {
        throw new BadRequestException("Les achats de cartes cadeaux sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    // Admin endpoints
    @GetMapping("/admin/gift-cards/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GiftCard> verifyGiftCard(@RequestParam String token) {
        return ResponseEntity.ok(giftCardService.verifyGiftCardForAdmin(token));
    }

    @PostMapping("/admin/gift-cards/{giftCardId}/mark-used")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> markServiceGiftCardAsUsed(@PathVariable String giftCardId,
                                                                         Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        giftCardService.markServiceGiftCardAsUsed(giftCardId, admin.getId());
        return ResponseEntity.ok(Map.of("message", "Carte cadeau de service marquée comme utilisée"));
    }

    @PostMapping("/admin/gift-cards/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> expireGiftCards() {
        giftCardService.expireGiftCards();
        return ResponseEntity.ok(Map.of("message", "Cartes expirées traitées avec succès"));
    }

    @GetMapping("/admin/gift-cards/payment-intent/{paymentIntentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getGiftCardByPaymentIntent(@PathVariable String paymentIntentId) {
        try {
            String paymentStatus = stripeService.getPaymentStatus(paymentIntentId);
            GiftCard giftCard = giftCardService.getGiftCardByPaymentIntent(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("gift_card", giftCard);
            response.put("payment_status", paymentStatus);

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}