package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    // Stripe payment intent ID for verification
    private String paymentIntentId;

    // For other payment methods
    private String paymentMethodId;
}