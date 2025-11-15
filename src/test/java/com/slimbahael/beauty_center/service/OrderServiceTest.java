package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.CheckoutRequest;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.Cart;
import com.slimbahael.beauty_center.model.Order;
import com.slimbahael.beauty_center.model.Product;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository; // not used directly but required by constructor
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private BalanceTransactionRepository balanceTransactionRepository;
    @Mock private CartService cartService;
    @Mock private SmsService smsService;
    @Mock private EmailService emailService;
    @Mock private StripeService stripeService;
    @Mock private BalanceService balanceService;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Product product;
    private Cart cart;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id("cust-1")
                .email("customer@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("+15551234567")
                .role("CUSTOMER")
                .build();

        product = Product.builder()
                .id("prod-1")
                .name("Serum")
                .price(new BigDecimal("20.00"))
                .stockQuantity(5)
                .imageUrls(List.of("http://img"))
                .build();

        Cart.CartItem cartItem = Cart.CartItem.builder()
                .productId("prod-1")
                .productName("Serum")
                .quantity(2)
                .unitPrice(new BigDecimal("20.00"))
                .totalPrice(new BigDecimal("40.00"))
                .build();

        cart = Cart.builder()
                .customerId(customer.getId())
                .items(List.of(cartItem))
                .subtotal(new BigDecimal("40.00"))
                .build();

        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setFullName("Jane Doe");
        checkoutRequest.setAddressLine1("123 Main St");
        checkoutRequest.setCity("Paris");
        checkoutRequest.setState("IDF");
        checkoutRequest.setPostalCode("75000");
        checkoutRequest.setCountry("France");
        checkoutRequest.setPhoneNumber(customer.getPhoneNumber());
        checkoutRequest.setPaymentMethod("BALANCE");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutWithBalanceCreatesOrderAndSendsNotifications() {
        BigDecimal total = new BigDecimal("45.00"); // subtotal 40 + shipping 5

        when(balanceService.hasInsufficientBalance(customer.getId(), total)).thenReturn(false);
        BalanceTransaction transaction = BalanceTransaction.builder()
                .id("txn-1")
                .userId(customer.getId())
                .build();
        when(balanceService.processBalancePayment(eq(customer.getId()), eq(total), any(), isNull()))
                .thenReturn(transaction);

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-1");
            return order;
        });
        when(balanceTransactionRepository.save(any(BalanceTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.checkoutWithBalance(
                checkoutRequest, customer, cart,
                cart.getSubtotal(), BigDecimal.ZERO,
                new BigDecimal("5.00"), total);

        assertThat(response.getId()).isEqualTo("order-1");
        assertThat(response.getTotal()).isEqualByComparingTo(total);
        assertThat(response.getPaymentMethod()).isEqualTo("BALANCE");

        verify(balanceService).processBalancePayment(customer.getId(), total, "Payment pour une commande", null);
        verify(balanceTransactionRepository).save(argThat(tx -> "order-1".equals(tx.getOrderId())));
        verify(cartService).clearCart();
        verify(smsService).sendSms(eq(customer.getPhoneNumber()), any());
        verify(emailService).sendOrderConfirmationEmail(eq(customer.getEmail()), any());
        verify(emailService).sendNewOrderNotificationToAdmin(any());
    }

    @Test
    void checkoutWithBalanceFailsWhenInsufficientBalance() {
        BigDecimal total = new BigDecimal("45.00");
        when(balanceService.hasInsufficientBalance(customer.getId(), total)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> orderService.checkoutWithBalance(
                checkoutRequest, customer, cart,
                cart.getSubtotal(), BigDecimal.ZERO,
                new BigDecimal("5.00"), total));

        verify(balanceService, never()).processBalancePayment(any(), any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatusCancelsOrderRestoresStockAndNotifies() {
        Order order = Order.builder()
                .id("order-2")
                .customerId(customer.getId())
                .items(List.of(Order.OrderItem.builder()
                        .productId("prod-1")
                        .productName("Serum")
                        .quantity(1)
                        .unitPrice(new BigDecimal("20.00"))
                        .totalPrice(new BigDecimal("20.00"))
                        .build()))
                .shippingAddress(Order.ShippingAddress.builder()
                        .fullName("Jane Doe")
                        .addressLine1("123 Main St")
                        .city("Paris")
                        .state("IDF")
                        .postalCode("75000")
                        .country("France")
                        .phoneNumber(customer.getPhoneNumber())
                        .build())
                .orderStatus("PROCESSING")
                .paymentStatus("PAID")
                .stripePaymentIntentId("pi_1")
                .createdAt(new java.util.Date())
                .updatedAt(new java.util.Date())
                .build();

        User admin = User.builder()
                .id("admin-1")
                .email("admin@example.com")
                .role("ADMIN")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(admin.getEmail(), null));

        when(orderRepository.findById("order-2")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.updateOrderStatus("order-2", "CANCELLED");

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(product.getStockQuantity()).isEqualTo(6); // restored

        verify(smsService).sendSms(eq(customer.getPhoneNumber()), any());
        verify(emailService).sendCancelledOrderNotificationToAdmin(any(), eq("Commande annulée par l'administrateur"));
    }
}
