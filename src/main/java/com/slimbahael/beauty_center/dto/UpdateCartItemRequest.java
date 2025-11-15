package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;

@Data
public class UpdateCartItemRequest {

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
}