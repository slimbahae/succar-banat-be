package com.slimbahael.beauty_center.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<String> imageUrls;
    private List<String> tags;
    private String brand;
    private String sku;
    private boolean featured;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;
    private List<ProductSpecificationDto> specifications;
    private BigDecimal discountPercentage;
    private Date discountStartDate;
    private Date discountEndDate;
    // Rating information
    private Double averageRating;
    private Long totalRatings;
    private BigDecimal finalPrice; // Price after applying discount

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSpecificationDto {
        private String name;
        private String value;
    }
}