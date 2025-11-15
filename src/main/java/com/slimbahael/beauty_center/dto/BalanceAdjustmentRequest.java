package com.slimbahael.beauty_center.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceAdjustmentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // Optional: Transaction type for better categorization
    private String transactionType; // CREDIT, DEBIT, REFUND, etc.

    // Optional: Reference to related entity
    private String referenceId;

    // Optional: Admin notes
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}