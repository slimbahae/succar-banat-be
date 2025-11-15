package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceAddonRequest {

    @NotBlank(message = "Add-on name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private BigDecimal price;

    @PositiveOrZero(message = "Additional duration must be zero or positive")
    private Integer additionalDuration;

    private List<String> compatibleServiceIds;

    private boolean active = true;
}