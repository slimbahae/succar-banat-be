package com.slimbahael.beauty_center.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    @Indexed(unique = true)
    private String reviewId; // Unique identifier from Google

    private String userName;
    private String userProfileImage;
    private String userLocalGuideInfo;
    private int rating;
    private String reviewText;
    private LocalDateTime reviewDate;
    private String ownerResponse;
    private LocalDateTime ownerResponseDate;
    private int likesCount;
    private List<String> reviewImages;
    private String source; // "GOOGLE_MAPS"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
}