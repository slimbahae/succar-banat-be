package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal finalPrice; // Price after discount
    private Integer duration;
    private List<String> imageUrls;
    private List<String> assignedStaffIds;
    private boolean featured;
    private boolean active;
    private boolean availableMorning;
    private boolean availableEvening;
    private Date createdAt;
    private Date updatedAt;
    private BigDecimal discountPercentage;
    private Date discountStartDate;
    private Date discountEndDate;
}