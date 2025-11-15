package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByOrderStatus(String orderStatus);

    List<Order> findByPaymentStatus(String paymentStatus);

    List<Order> findByCreatedAtBetween(Date startDate, Date endDate);

    // Stripe-specific methods
    default Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return findAll().stream()
                .filter(order -> stripePaymentIntentId.equals(order.getStripePaymentIntentId()))
                .findFirst();
    }

    default List<Order> findByStripePaymentIntentIdIsNotNull() {
        return findAll().stream()
                .filter(order -> order.getStripePaymentIntentId() != null)
                .toList();
    }

    default Optional<Order> findByStripeChargeId(String stripeChargeId) {
        return findAll().stream()
                .filter(order -> stripeChargeId.equals(order.getStripeChargeId()))
                .findFirst();
    }
}