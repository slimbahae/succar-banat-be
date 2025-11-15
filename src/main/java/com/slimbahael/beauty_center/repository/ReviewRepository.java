package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    Optional<Review> findByReviewId(String reviewId);

    List<Review> findByIsActiveTrueOrderByReviewDateDesc();

    List<Review> findByRatingGreaterThanEqualAndIsActiveTrueOrderByReviewDateDesc(int rating);

    @Query("{ 'createdAt' : { $gte: ?0 } }")
    List<Review> findReviewsCreatedAfter(LocalDateTime date);

    long countByIsActiveTrue();

    @Query("{ 'isActive': true }")
    List<Review> findActiveReviewsWithLimit(int limit);
}