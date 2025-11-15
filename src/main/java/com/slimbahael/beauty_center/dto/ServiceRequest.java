package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ServiceRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer duration;

    private List<String> imageUrls;

    private List<String> assignedStaffIds;

    private boolean featured;

    private boolean active = true;

    private boolean availableMorning = true;

    private boolean availableEvening = true;

    private BigDecimal discountPercentage;
    private Date discountStartDate;
    private Date discountEndDate;
}