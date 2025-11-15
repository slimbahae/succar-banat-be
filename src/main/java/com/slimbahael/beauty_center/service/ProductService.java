package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.service.RatingService;
import com.slimbahael.beauty_center.dto.ProductRequest;
import com.slimbahael.beauty_center.dto.ProductResponse;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Product;
import com.slimbahael.beauty_center.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final RatingService ratingService;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapProductToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActiveIsTrue()
                .stream()
                .map(this::mapProductToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findByFeaturedIsTrue()
                .stream()
                .map(this::mapProductToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::mapProductToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapProductToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapProductToResponse(product);
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .category(productRequest.getCategory())
                .price(productRequest.getPrice())
                .stockQuantity(productRequest.getStockQuantity())
                .imageUrls(productRequest.getImageUrls())
                .tags(productRequest.getTags())
                .brand(productRequest.getBrand())
                .sku(productRequest.getSku())
                .featured(productRequest.isFeatured())
                .active(productRequest.isActive())
                .createdAt(new Date())
                .updatedAt(new Date())
                .discountPercentage(productRequest.getDiscountPercentage())
                .discountStartDate(productRequest.getDiscountStartDate())
                .discountEndDate(productRequest.getDiscountEndDate())
                .build();

        // Map specifications if they exist
        if (productRequest.getSpecifications() != null && !productRequest.getSpecifications().isEmpty()) {
            List<Product.ProductSpecification> specifications = productRequest.getSpecifications().stream()
                    .map(spec -> Product.ProductSpecification.builder()
                            .name(spec.getName())
                            .value(spec.getValue())
                            .build())
                    .collect(Collectors.toList());
            product.setSpecifications(specifications);
        } else {
            product.setSpecifications(new ArrayList<>());
        }

        Product savedProduct = productRepository.save(product);
        ProductResponse productResponse = mapProductToResponse(savedProduct);

        // 🔥 NEW: Send admin notification
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            emailService.sendProductAddedNotificationToAdmin(productResponse, userEmail);
        } catch (Exception e) {
            log.error("Failed to send product added notification to admin for product {}: {}",
                    savedProduct.getName(), e.getMessage());
        }

        return productResponse;
    }

    public ProductResponse updateProduct(String id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        existingProduct.setName(productRequest.getName());
        existingProduct.setDescription(productRequest.getDescription());
        existingProduct.setCategory(productRequest.getCategory());
        existingProduct.setPrice(productRequest.getPrice());
        existingProduct.setStockQuantity(productRequest.getStockQuantity());
        existingProduct.setImageUrls(productRequest.getImageUrls());
        existingProduct.setTags(productRequest.getTags());
        existingProduct.setBrand(productRequest.getBrand());
        existingProduct.setSku(productRequest.getSku());
        existingProduct.setFeatured(productRequest.isFeatured());
        existingProduct.setActive(productRequest.isActive());
        existingProduct.setUpdatedAt(new Date());
        existingProduct.setDiscountPercentage(productRequest.getDiscountPercentage());
        existingProduct.setDiscountStartDate(productRequest.getDiscountStartDate());
        existingProduct.setDiscountEndDate(productRequest.getDiscountEndDate());

        // Map specifications if they exist
        if (productRequest.getSpecifications() != null && !productRequest.getSpecifications().isEmpty()) {
            List<Product.ProductSpecification> specifications = productRequest.getSpecifications().stream()
                    .map(spec -> Product.ProductSpecification.builder()
                            .name(spec.getName())
                            .value(spec.getValue())
                            .build())
                    .collect(Collectors.toList());
            existingProduct.setSpecifications(specifications);
        } else {
            existingProduct.setSpecifications(new ArrayList<>());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        ProductResponse productResponse = mapProductToResponse(updatedProduct);

        // 🔥 NEW: Check for low stock and notify admin
        try {
            if (updatedProduct.isActive() && updatedProduct.getStockQuantity() <= 5) { // Low stock threshold
                emailService.sendLowStockNotificationToAdmin(productResponse);
            }
        } catch (Exception e) {
            log.error("Failed to send low stock notification to admin for product {}: {}",
                    updatedProduct.getName(), e.getMessage());
        }

        return productResponse;
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    // 🔥 NEW: Add method to check all products for low stock (can be called by scheduler)
    public void checkLowStockProducts() {
        List<Product> lowStockProducts = productRepository.findAll().stream()
                .filter(product -> product.isActive() && product.getStockQuantity() <= 5)
                .collect(Collectors.toList());

        for (Product product : lowStockProducts) {
            try {
                ProductResponse productResponse = mapProductToResponse(product);
                emailService.sendLowStockNotificationToAdmin(productResponse);
            } catch (Exception e) {
                log.error("Failed to send low stock notification for product {}: {}",
                        product.getName(), e.getMessage());
            }
        }
    }

    // Helper method to map Product entity to ProductResponse DTO
    private ProductResponse mapProductToResponse(Product product) {
        BigDecimal finalPrice = product.getPrice();

        // Calculate discount if applicable
        if (product.getDiscountPercentage() != null &&
                product.getDiscountStartDate() != null &&
                product.getDiscountEndDate() != null) {

            Date now = new Date();
            if (now.after(product.getDiscountStartDate()) && now.before(product.getDiscountEndDate())) {
                BigDecimal discountAmount = product.getPrice()
                        .multiply(product.getDiscountPercentage())
                        .divide(new BigDecimal("100"));
                finalPrice = product.getPrice().subtract(discountAmount);
            }
        }

        // Map specifications
        List<ProductResponse.ProductSpecificationDto> specificationDtos = new ArrayList<>();
        if (product.getSpecifications() != null) {
            specificationDtos = product.getSpecifications().stream()
                    .map(spec -> ProductResponse.ProductSpecificationDto.builder()
                            .name(spec.getName())
                            .value(spec.getValue())
                            .build())
                    .collect(Collectors.toList());
        }

        // Get rating information
        Double averageRating = ratingService.getProductAverageRating(product.getId());
        Long totalRatings = ratingService.getProductRatingCount(product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .finalPrice(finalPrice)
                .stockQuantity(product.getStockQuantity())
                .imageUrls(product.getImageUrls())
                .tags(product.getTags())
                .brand(product.getBrand())
                .sku(product.getSku())
                .featured(product.isFeatured())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .specifications(specificationDtos)
                .discountPercentage(product.getDiscountPercentage())
                .discountStartDate(product.getDiscountStartDate())
                .discountEndDate(product.getDiscountEndDate())
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .build();
    }
}