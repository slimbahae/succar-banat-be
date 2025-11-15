package com.slimbahael.beauty_center.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "stripe.webhook.enabled", havingValue = "true", matchIfMissing = false)
public class StripeWebhookController {

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        log.info("Received Stripe webhook (placeholder implementation)");

        // Placeholder implementation - will be activated when webhook is enabled
        // For now, just acknowledge receipt
        return ResponseEntity.ok("Webhook received");
    }
}