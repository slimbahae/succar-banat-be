package com.slimbahael.beauty_center.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "ratings")
public class Rating {

    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed
    private String customerId;

    private String customerName;

    private Integer rating; // 1-5 stars

    private String comment;

    private Date createdAt;

    private Date updatedAt;

    // Flag to check if this rating is verified (customer actually bought the product)
    private boolean verified;
}