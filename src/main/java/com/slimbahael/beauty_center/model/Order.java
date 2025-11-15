package com.slimbahael.beauty_center.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private List<OrderItem> items;

    private ShippingAddress shippingAddress;

    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal total;

    private String paymentMethod;
    private String paymentStatus; // "PENDING", "PAID", "FAILED", "REFUNDED"

    private String orderStatus; // "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"

    // Stripe payment fields
    private String stripePaymentIntentId;
    private String stripePaymentMethodId;
    private String stripeChargeId;

    private Date createdAt;
    private Date updatedAt;




    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddress {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phoneNumber;
    }
}