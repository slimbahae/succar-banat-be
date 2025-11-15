package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByActiveIsTrue();

    List<Product> findByFeaturedIsTrue();

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByActiveIsTrueAndStockQuantityGreaterThan(Integer minimumStock);

    List<Product> findByTagsContaining(String tag);
}