package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.PaymentIntentRequest;
import com.slimbahael.beauty_center.dto.PaymentIntentResponse;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    /**
     * Create a payment intent for the given amount in EUR
     */

    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) {
        try {
            long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> metadata = new HashMap<>();
            if (request.getOrderId() != null) {
                metadata.put("order_id", request.getOrderId());
            }
            if (request.getCustomerEmail() != null) {
                metadata.put("customer_email", request.getCustomerEmail());
            }
            if (request.getUserId() != null) {
                // use snake_case to match what you read back later
                metadata.put("user_id", request.getUserId());
            }

            log.info("Building Stripe PaymentIntent with metadata → {}", metadata);

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata);

            if (request.getDescription() != null) {
                paramsBuilder.setDescription(request.getDescription());
            }

            PaymentIntentCreateParams params = paramsBuilder.build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("Created payment intent: {} for amount: €{}", paymentIntent.getId(), request.getAmount());

            return PaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .amount(paymentIntent.getAmount())
                    .currency(paymentIntent.getCurrency())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create payment intent: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create payment intent: " + e.getMessage());
        }
    }


    /**
     * Retrieve a payment intent by ID
     */
    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Failed to retrieve payment intent {}: {}", paymentIntentId, e.getMessage());
            throw new BadRequestException("Failed to retrieve payment intent: " + e.getMessage());
        }
    }

    /**
     * Confirm a payment intent (for server-side confirmation)
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethodId)
                    .build();

            return paymentIntent.confirm(params);
        } catch (StripeException e) {
            log.error("Failed to confirm payment intent {}: {}", paymentIntentId, e.getMessage());
            throw new BadRequestException("Failed to confirm payment: " + e.getMessage());
        }
    }

    /**
     * Cancel a payment intent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.cancel();
        } catch (StripeException e) {
            log.error("Failed to cancel payment intent {}: {}", paymentIntentId, e.getMessage());
            throw new BadRequestException("Failed to cancel payment: " + e.getMessage());
        }
    }

    /**
     * Check if a payment intent is succeeded
     */
    public boolean isPaymentSucceeded(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(paymentIntent.getStatus());
        } catch (StripeException e) {
            log.error("Failed to check payment status for {}: {}", paymentIntentId, e.getMessage());
            return false;
        }
    }

    /**
     * Get payment status
     */
    public String getPaymentStatus(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.getStatus();
        } catch (StripeException e) {
            log.error("Failed to get payment status for {}: {}", paymentIntentId, e.getMessage());
            return "unknown";
        }
    }
}
