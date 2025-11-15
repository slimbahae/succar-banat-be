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

    @PostMapping("/customer/gift-cards/create-payment-intent")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentIntentResponse> createGiftCardPaymentIntent(
            @Valid @RequestBody GiftCardPurchaseRequest request,
            Authentication authentication) {

        try {
            // Get current user
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Build payment intent
            PaymentIntentRequest paymentRequest = new PaymentIntentRequest();
            paymentRequest.setAmount(request.getAmount());
            paymentRequest.setDescription("Gift Card Purchase - " + request.getType() + " - " + request.getAmount() + "€");
            paymentRequest.setCustomerEmail(request.getPurchaserEmail());
            paymentRequest.setUserId(user.getId());

            // Add metadata for tracking
            Map<String, String> metadata = new HashMap<>();
            metadata.put("gift_card_type", request.getType());
            metadata.put("purchaser_email", request.getPurchaserEmail());
            metadata.put("recipient_email", request.getRecipientEmail());
            metadata.put("purchaser_name", request.getPurchaserName());
            metadata.put("recipient_name", request.getRecipientName());
            if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
                metadata.put("message", request.getMessage());
            }
            // **Attach metadata to the request**
            paymentRequest.setMetadata(metadata);

            PaymentIntentResponse response = stripeService.createPaymentIntent(paymentRequest);

            log.info("Payment intent created for gift card: {} - Amount: {}€",
                    response.getPaymentIntentId(), request.getAmount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create payment intent for gift card: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create payment intent: " + e.getMessage());
        }
    }

    @PostMapping("/customer/gift-cards/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<GiftCard> purchaseGiftCard(
            @Valid @RequestBody GiftCardPurchaseRequest request,
            Authentication authentication) {

        // Ensure the purchaserEmail matches the authenticated user
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        request.setPurchaserEmail(user.getEmail());

        try {
            GiftCard createdGiftCard = giftCardService.createGiftCard(request);
            log.info("Gift card purchased successfully: {} by {}",
                    createdGiftCard.getId(), user.getEmail());
            return ResponseEntity.ok(createdGiftCard);
        } catch (Exception e) {
            log.error("Failed to purchase gift card: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/customer/gift-cards/payment-status/{paymentIntentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getGiftCardPaymentStatus(@PathVariable String paymentIntentId) {
        try {
            String status = stripeService.getPaymentStatus(paymentIntentId);
            Map<String, Object> response = new HashMap<>();
            response.put("payment_intent_id", paymentIntentId);
            response.put("status", status);
            response.put("is_succeeded", "succeeded".equals(status));

            try {
                GiftCard giftCard = giftCardService.getGiftCardByPaymentIntent(paymentIntentId);
                response.put("gift_card_id", giftCard.getId());
                response.put("gift_card_status", giftCard.getStatus());
            } catch (ResourceNotFoundException e) {
                response.put("gift_card_id", null);
                response.put("gift_card_status", null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get payment status for: {}", paymentIntentId, e);
            throw new BadRequestException("Failed to get payment status: " + e.getMessage());
        }
    }

    @PostMapping("/customer/gift-cards/redeem")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BalanceTransaction> redeemGiftCard(
            @Valid @RequestBody GiftCardRedemptionRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddressHeader,
            @RequestHeader(value = "X-Real-IP", required = false) String realIpHeader,
            @RequestHeader(value = "Remote_Addr", required = false) String remoteAddrHeader) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String ipAddress = ipAddressHeader != null ? ipAddressHeader
                : realIpHeader != null ? realIpHeader
                : remoteAddrHeader != null ? remoteAddrHeader
                : "UNKNOWN";

        BalanceTransaction transaction = giftCardService
                .redeemGiftCard(request.getCode(), user.getId(), ipAddress);
        return ResponseEntity.ok(transaction);
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
        try {
            stripeService.cancelPaymentIntent(paymentIntentId);
            giftCardService.cancelGiftCardForFailedPayment(paymentIntentId);
            log.info("Gift card payment cancelled: {}", paymentIntentId);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment cancelled successfully",
                    "payment_intent_id", paymentIntentId
            ));
        } catch (Exception e) {
            log.error("Failed to cancel gift card payment: {}", paymentIntentId, e);
            throw new BadRequestException("Failed to cancel payment: " + e.getMessage());
        }
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