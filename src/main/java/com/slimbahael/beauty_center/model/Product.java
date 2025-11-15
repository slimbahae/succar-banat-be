package com.slimbahael.beauty_center.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    @Indexed
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

    // Specifications as a flexible structure
    private List<ProductSpecification> specifications;

    // For discount management
    private BigDecimal discountPercentage;
    private Date discountStartDate;
    private Date discountEndDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSpecification {
        private String name;
        private String value;
    }
}