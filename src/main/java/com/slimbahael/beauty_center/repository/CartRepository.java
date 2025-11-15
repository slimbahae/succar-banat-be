package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByCustomerId(String customerId);

    void deleteByCustomerId(String customerId);
}