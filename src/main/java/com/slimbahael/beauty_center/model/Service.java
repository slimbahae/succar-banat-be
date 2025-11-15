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
@Document(collection = "services")
public class Service {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    @Indexed
    private String category;

    private BigDecimal price;

    // Duration in minutes
    private Integer duration;

    private List<String> imageUrls;

    // List of staff IDs who can perform this service
    private List<String> assignedStaffIds;

    private boolean featured;

    private boolean active;

    private Date createdAt;

    private Date updatedAt;

    // For service availability
    private boolean availableMorning;
    private boolean availableEvening;

    // For discount management
    private BigDecimal discountPercentage;
    private Date discountStartDate;
    private Date discountEndDate;
}