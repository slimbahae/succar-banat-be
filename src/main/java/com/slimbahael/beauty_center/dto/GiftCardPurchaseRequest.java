package com.slimbahael.beauty_center.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardPurchaseRequest {

    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "1.00", message = "Le montant minimum est de 1€")
    private BigDecimal amount;

    @NotBlank(message = "Le type de carte cadeau est requis")
    @Pattern(regexp = "BALANCE|SERVICE", message = "Type invalide")
    private String type;

    @Email(message = "Email acheteur invalide")
    @NotBlank(message = "Email acheteur requis")
    private String purchaserEmail;

    @NotBlank(message = "Nom acheteur requis")
    @Size(min = 1, max = 100, message = "Nom acheteur entre 1 et 100 caractères")
    private String purchaserName;

    @Email(message = "Email destinataire invalide")
    @NotBlank(message = "Email destinataire requis")
    private String recipientEmail;

    @NotBlank(message = "Nom destinataire requis")
    @Size(min = 1, max = 100, message = "Nom destinataire entre 1 et 100 caractères")
    private String recipientName;

    @Size(max = 500, message = "Message trop long")
    private String message;

    private String paymentIntentId; // Now required for security
}