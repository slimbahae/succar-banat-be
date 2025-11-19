package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.CheckoutSessionResponse;
import com.slimbahael.beauty_center.dto.GiftCardPurchaseRequest;
import com.slimbahael.beauty_center.dto.GiftCardRedemptionRequest;
import com.slimbahael.beauty_center.model.GiftCard;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.service.GiftCardService;
import com.slimbahael.beauty_center.service.StripeService;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    // GIFT CARD PURCHASE - Using Stripe Checkout for reservations only (not balance)

    @PostMapping("/customer/gift-cards/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CheckoutSessionResponse> createGiftCardCheckoutSession(
            @Valid @RequestBody GiftCardPurchaseRequest request,
            Authentication authentication) {

        // Get current user
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate request
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant doit être supérieur à zéro");
        }

        // Set purchaser info
        request.setPurchaserEmail(user.getEmail());
        if (request.getPurchaserName() == null || request.getPurchaserName().isEmpty()) {
            request.setPurchaserName(user.getFirstName() + " " + user.getLastName());
        }

        try {
            // Create pending gift card first to get the ID
            GiftCard giftCard = giftCardService.createPendingGiftCard(request);

            // Create Stripe Checkout Session
            String description = "Carte Cadeau - " + request.getAmount() + "€ pour Réservations";
            Session session = stripeService.createGiftCardCheckoutSession(
                    giftCard.getId(),
                    request.getAmount(),
                    user.getEmail(),
                    description
            );

            log.info("Created checkout session for gift card: {} - Amount: {}€",
                    giftCard.getId(), request.getAmount());

            return ResponseEntity.ok(CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .reservationId(giftCard.getId())  // Using for gift card ID
                    .build());

        } catch (Exception e) {
            log.error("Failed to create gift card checkout session: {}", e.getMessage(), e);
            throw new BadRequestException("Échec de la création de la session de paiement: " + e.getMessage());
        }
    }

    @PostMapping("/customer/gift-cards/verify-payment/{sessionId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> verifyGiftCardPayment(
            @PathVariable String sessionId,
            Authentication authentication) {

        try {
            // Verify and complete gift card purchase
            GiftCard giftCard = giftCardService.completeGiftCardPurchase(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("gift_card_id", giftCard.getId());
            response.put("amount", giftCard.getAmount());
            response.put("message", "Carte cadeau achetée avec succès! Le code a été envoyé par email.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to verify gift card payment: {}", e.getMessage(), e);
            throw new BadRequestException("Échec de la vérification du paiement: " + e.getMessage());
        }
    }

    // GIFT CARD REDEMPTION - Verify card for use with reservations (not balance)
    @PostMapping("/customer/gift-cards/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> verifyGiftCardForReservation(
            @Valid @RequestBody GiftCardRedemptionRequest request,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            // Verify the gift card is valid and can be used
            GiftCard giftCard = giftCardService.verifyGiftCardForUse(request.getCode(), user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("gift_card_id", giftCard.getId());
            response.put("amount", giftCard.getAmount());
            response.put("recipient_email", giftCard.getRecipientEmail());
            response.put("message", "Carte cadeau valide! Peut être utilisée pour les réservations.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to verify gift card: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping("/customer/gift-cards/apply-to-reservation/{reservationId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> applyGiftCardToReservation(
            @PathVariable String reservationId,
            @Valid @RequestBody GiftCardRedemptionRequest request,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            // Apply gift card to reservation
            Map<String, Object> result = giftCardService.applyGiftCardToReservation(
                    request.getCode(),
                    reservationId,
                    user.getEmail()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to apply gift card to reservation: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
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