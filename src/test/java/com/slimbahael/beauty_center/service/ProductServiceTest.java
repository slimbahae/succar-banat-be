package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.ProductRequest;
import com.slimbahael.beauty_center.dto.ProductResponse;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Product;
import com.slimbahael.beauty_center.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RatingService ratingService;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProductByIdReturnsMappedResponseWithDiscountAndRatings() {
        Product product = Product.builder()
                .id("prod-1")
                .name("Hydrating Serum")
                .description("Locks in moisture")
                .category("skincare")
                .price(new BigDecimal("100.00"))
                .stockQuantity(12)
                .imageUrls(List.of("serum.png"))
                .tags(List.of("hydrating", "vegan"))
                .brand("GlowCo")
                .sku("SKU-001")
                .featured(true)
                .active(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .discountPercentage(new BigDecimal("10"))
                .discountStartDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                .discountEndDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .specifications(List.of(Product.ProductSpecification.builder()
                        .name("size")
                        .value("30ml")
                        .build()))
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(ratingService.getProductAverageRating("prod-1")).thenReturn(4.5);
        when(ratingService.getProductRatingCount("prod-1")).thenReturn(12L);

        ProductResponse response = productService.getProductById("prod-1");

        assertEquals("Hydrating Serum", response.getName());
        assertEquals(0, new BigDecimal("90.00").compareTo(response.getFinalPrice()));
        assertEquals(4.5, response.getAverageRating());
        assertEquals(12L, response.getTotalRatings());
        assertThat(response.getSpecifications()).hasSize(1);

        verify(productRepository).findById("prod-1");
        verify(ratingService).getProductAverageRating("prod-1");
        verify(ratingService).getProductRatingCount("prod-1");
    }

    @Test
    void getProductByIdThrowsWhenMissing() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById("missing"));
    }

    @Test
    void createProductPersistsEntityAndNotifiesAdmin() {
        ProductRequest request = buildProductRequest();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId("generated-id");
            return saved;
        });
        doReturn(0.0).when(ratingService).getProductAverageRating("generated-id");
        doReturn(0L).when(ratingService).getProductRatingCount("generated-id");

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ProductResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals("Glow Mask", savedProduct.getName());
        assertEquals(0, new BigDecimal("49.99").compareTo(savedProduct.getPrice()));
        assertEquals("generated-id", response.getId());

        verify(emailService).sendProductAddedNotificationToAdmin(response, "admin@example.com");
    }

    @Test
    void updateProductSendsLowStockNotificationWhenThresholdMet() {
        Product existingProduct = Product.builder()
                .id("prod-low")
                .name("Vitamin C Serum")
                .description("Brightening")
                .category("skincare")
                .price(new BigDecimal("80.00"))
                .stockQuantity(20)
                .active(true)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        when(productRepository.findById("prod-low")).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(0.0).when(ratingService).getProductAverageRating("prod-low");
        doReturn(0L).when(ratingService).getProductRatingCount("prod-low");

        ProductRequest request = buildProductRequest();
        request.setStockQuantity(3);
        request.setActive(true);

        ProductResponse response = productService.updateProduct("prod-low", request);

        verify(productRepository).save(existingProduct);
        assertEquals(3, existingProduct.getStockQuantity());
        verify(emailService).sendLowStockNotificationToAdmin(response);
    }

    @Test
    void updateProductDoesNotNotifyWhenStockHealthy() {
        Product existingProduct = Product.builder()
                .id("prod-ok")
                .name("Night Cream")
                .category("skincare")
                .price(new BigDecimal("70.00"))
                .stockQuantity(10)
                .active(true)
                .build();

        when(productRepository.findById("prod-ok")).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(0.0).when(ratingService).getProductAverageRating("prod-ok");
        doReturn(0L).when(ratingService).getProductRatingCount("prod-ok");

        ProductRequest request = buildProductRequest();
        request.setStockQuantity(8);

        productService.updateProduct("prod-ok", request);

        verify(emailService, never()).sendLowStockNotificationToAdmin(any(ProductResponse.class));
    }

    private ProductRequest buildProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("Glow Mask");
        request.setDescription("Revitalizes dull skin");
        request.setCategory("skincare");
        request.setPrice(new BigDecimal("49.99"));
        request.setStockQuantity(15);
        request.setImageUrls(List.of("mask.jpg"));
        request.setTags(List.of("mask", "glow"));
        request.setBrand("GlowCo");
        request.setSku("SKU-XYZ");
        request.setFeatured(true);
        request.setActive(true);
        return request;
    }
}
