package com.slimbahael.beauty_center.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyEmailRequest {
    @NotBlank(message = "Token is required")
    private String token;
}