package com.slimbahael.beauty_center.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardRedemptionRequest {

    @NotBlank(message = "Code de la carte cadeau requis")
    @Size(min = 10, max = 50, message = "Code invalide")
    private String code;
}