package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private String id;
    private String productId;
    private String customerId;
    private String customerName;
    private Integer rating;
    private String comment;
    private Date createdAt;
    private Date updatedAt;
    private boolean verified;
}