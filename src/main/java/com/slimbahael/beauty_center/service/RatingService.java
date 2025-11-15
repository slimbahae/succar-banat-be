package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.RatingRequest;
import com.slimbahael.beauty_center.dto.RatingResponse;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Order;
import com.slimbahael.beauty_center.model.Product;
import com.slimbahael.beauty_center.model.Rating;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.OrderRepository;
import com.slimbahael.beauty_center.repository.ProductRepository;
import com.slimbahael.beauty_center.repository.RatingRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public List<RatingResponse> getProductRatings(String productId) {
        return ratingRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapRatingToResponse)
                .collect(Collectors.toList());
    }

    public RatingResponse createOrUpdateRating(RatingRequest request) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if customer already rated this product
        Optional<Rating> existingRating = ratingRepository.findByProductIdAndCustomerId(
                request.getProductId(), customer.getId());

        Rating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setRating(request.getRating());
            rating.setComment(request.getComment());
            rating.setUpdatedAt(new Date());
            log.info("Updating existing rating for product {} by customer {}",
                    request.getProductId(), customer.getId());
        } else {
            // Create new rating
            rating = Rating.builder()
                    .productId(request.getProductId())
                    .customerId(customer.getId())
                    .customerName(customer.getFirstName() + " " + customer.getLastName())
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .verified(hasCustomerPurchasedProduct(customer.getId(), request.getProductId()))
                    .build();
            log.info("Creating new rating for product {} by customer {}",
                    request.getProductId(), customer.getId());
        }

        Rating savedRating = ratingRepository.save(rating);
        return mapRatingToResponse(savedRating);
    }

    public void deleteRating(String ratingId) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));

        // Only the customer who created the rating or admin can delete it
        if (!rating.getCustomerId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new BadRequestException("You can only delete your own ratings");
        }

        ratingRepository.delete(rating);
        log.info("Rating {} deleted by user {}", ratingId, user.getId());
    }

    public Double getProductAverageRating(String productId) {
        List<Rating> ratings = ratingRepository.findByProductId(productId);
        if (ratings.isEmpty()) {
            return 0.0;
        }

        double average = ratings.stream()
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);

        return Math.round(average * 10.0) / 10.0; // Round to 1 decimal place
    }

    public long getProductRatingCount(String productId) {
        return ratingRepository.countByProductId(productId);
    }

    // Check if customer has purchased this product (for verified reviews)
    private boolean hasCustomerPurchasedProduct(String customerId, String productId) {
        List<Order> customerOrders = orderRepository.findByCustomerId(customerId);

        return customerOrders.stream()
                .filter(order -> "DELIVERED".equals(order.getOrderStatus()) || "COMPLETED".equals(order.getOrderStatus()))
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.getProductId().equals(productId));
    }

    // Helper method to map Rating entity to RatingResponse DTO
    private RatingResponse mapRatingToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .productId(rating.getProductId())
                .customerId(rating.getCustomerId())
                .customerName(rating.getCustomerName())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .verified(rating.isVerified())
                .build();
    }
}