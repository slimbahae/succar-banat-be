package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Positive(message = "Stock quantity must be positive")
    private Integer stockQuantity;

    private List<String> imageUrls;

    private List<String> tags;

    private String brand;

    private String sku;

    private boolean featured;

    private boolean active = true;

    private List<ProductSpecificationDto> specifications;

    private BigDecimal discountPercentage;
    private Date discountStartDate;
    private Date discountEndDate;

    @Data
    public static class ProductSpecificationDto {
        private String name;
        private String value;
    }
}