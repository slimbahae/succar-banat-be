package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.RatingRequest;
import com.slimbahael.beauty_center.dto.RatingResponse;
import com.slimbahael.beauty_center.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // Public endpoint to get ratings for a product
    @GetMapping("/api/public/products/{productId}/ratings")
    public ResponseEntity<List<RatingResponse>> getProductRatings(@PathVariable String productId) {
        return ResponseEntity.ok(ratingService.getProductRatings(productId));
    }

    // Public endpoint to get product rating summary
    @GetMapping("/api/public/products/{productId}/rating-summary")
    public ResponseEntity<Map<String, Object>> getProductRatingSummary(@PathVariable String productId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("averageRating", ratingService.getProductAverageRating(productId));
        summary.put("totalRatings", ratingService.getProductRatingCount(productId));
        return ResponseEntity.ok(summary);
    }

    // Customer endpoint to create or update a rating
    @PostMapping("/api/customer/ratings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RatingResponse> createOrUpdateRating(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createOrUpdateRating(request));
    }

    // Customer endpoint to delete their own rating
    @DeleteMapping("/api/customer/ratings/{ratingId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteRating(@PathVariable String ratingId) {
        ratingService.deleteRating(ratingId);
        return ResponseEntity.noContent().build();
    }

    // Admin endpoint to delete any rating
    @DeleteMapping("/api/admin/ratings/{ratingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRatingAdmin(@PathVariable String ratingId) {
        ratingService.deleteRating(ratingId);
        return ResponseEntity.noContent().build();
    }
}