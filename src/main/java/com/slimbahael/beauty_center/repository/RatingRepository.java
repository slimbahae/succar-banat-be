package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {

    List<Rating> findByProductId(String productId);

    List<Rating> findByCustomerId(String customerId);

    Optional<Rating> findByProductIdAndCustomerId(String productId, String customerId);

    List<Rating> findByProductIdOrderByCreatedAtDesc(String productId);

    long countByProductId(String productId);
}