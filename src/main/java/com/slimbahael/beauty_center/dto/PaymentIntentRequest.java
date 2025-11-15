package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PaymentIntentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "usd";

    // Optional metadata for the payment
    private String orderId;
    private String customerEmail;
    private String description;
    private String userId;

    // ← new field
    private Map<String, String> metadata;
}
