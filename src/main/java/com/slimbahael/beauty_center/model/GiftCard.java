package com.slimbahael.beauty_center.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "gift_cards")
public class GiftCard {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank
    private String codeHash; // BCrypt hashed code

    @Indexed
    @NotBlank
    private String type; // BALANCE, SERVICE

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Indexed
    @NotBlank
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, REDEEMED, EXPIRED, CANCELLED

    @Email
    @NotBlank
    private String purchaserEmail;

    @NotBlank
    @Size(min = 1, max = 100)
    private String purchaserName;

    @Email
    @NotBlank
    private String recipientEmail;

    @NotBlank
    @Size(min = 1, max = 100)
    private String recipientName;

    @Size(max = 500)
    private String message; // Personal message from purchaser

    @Indexed
    @Builder.Default
    private Date createdAt = new Date();

    @Indexed
    private Date expirationDate;

    private Date redeemedAt;
    private String redeemedByUserId;

    // For Stripe integration
    private String paymentIntentId;
    private String purchaseOrderId;

    // For admin verification (Type 2)
    private String verificationToken; // For admin to verify without seeing code
    private Date lastVerificationAttempt;

    @Builder.Default
    private Integer verificationAttempts = 0;

    @Size(max = 1000)
    private String notes;

    // Security fields
    @Builder.Default
    private Integer redemptionAttempts = 0;
    private Date lastRedemptionAttempt;
    private String lastRedemptionIp;

    @Builder.Default
    private Boolean isLocked = false;
    private Date lockedAt;
    private String lockedReason;
}