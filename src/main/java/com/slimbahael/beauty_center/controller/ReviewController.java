package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.ApiResponse;
import com.slimbahael.beauty_center.model.Review;
import com.slimbahael.beauty_center.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "Reviews", description = "Review management endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get reviews", description = "Get paginated list of active reviews")
    public ResponseEntity<ApiResponse<List<Review>>> getReviews(
            @RequestParam(defaultValue = "10") int limit) {

        List<Review> reviews = reviewService.getActiveReviews(limit);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Reviews fetched successfully",
                reviews
        ));
    }

    @GetMapping("/rating/{minRating}")
    @Operation(summary = "Get reviews by rating", description = "Get reviews with minimum rating")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsByRating(
            @PathVariable int minRating) {

        List<Review> reviews = reviewService.getReviewsByRating(minRating);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Reviews fetched successfully",
                reviews
        ));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get review statistics", description = "Get review count and average rating")
    public ResponseEntity<ApiResponse<ReviewStats>> getReviewStats() {

        ReviewStats stats = new ReviewStats();
        stats.setTotalReviews(reviewService.getTotalReviewsCount());
        stats.setAverageRating(reviewService.getAverageRating());

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Review statistics fetched successfully",
                stats
        ));
    }

    @PostMapping("/fetch")
    @Operation(summary = "Manually fetch reviews", description = "Manually trigger review fetching from SerpAPI")
    public ResponseEntity<ApiResponse<List<Review>>> fetchReviews() {

        List<Review> newReviews = reviewService.fetchAndStoreReviews();

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Reviews fetched and stored successfully",
                newReviews
        ));
    }

    public static class ReviewStats {
        private long totalReviews;
        private double averageRating;

        // Getters and setters
        public long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    }
}