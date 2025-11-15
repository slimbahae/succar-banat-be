package com.slimbahael.beauty_center.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.slimbahael.beauty_center.dto.CheckoutRequest;
import com.slimbahael.beauty_center.dto.OrderResponse;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Cart;
import com.slimbahael.beauty_center.model.Order;
import com.slimbahael.beauty_center.model.Product;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BalanceTransactionRepository balanceTransactionRepository;
    private final CartService cartService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final StripeService stripeService;
    private final BalanceService balanceService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0"); // 10% tax
    private static final BigDecimal SHIPPING_COST = new BigDecimal("5.00"); // $5 shipping
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00"); // Free shipping over $50

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public byte[] generateInvoice(String orderId) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user is authorized to view this order
        if (!user.getRole().equals("ADMIN") && !order.getCustomerId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to view this order");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add company header
            Paragraph header = new Paragraph("Beauty Center - Invoice");
            header.setAlignment(Paragraph.ALIGN_CENTER);
            header.setSpacingAfter(20);
            document.add(header);

            // Add order details
            document.add(new Paragraph("Order #: " + order.getId()));
            document.add(new Paragraph("Date: " + order.getCreatedAt()));
            document.add(new Paragraph("Status: " + order.getOrderStatus()));

            // Add payment details
            if (order.getStripePaymentIntentId() != null) {
                document.add(new Paragraph("Payment ID: " + order.getStripePaymentIntentId()));
            }
            document.add(new Paragraph("\n"));

            // Add customer details
            document.add(new Paragraph("Customer Information:"));
            document.add(new Paragraph("Name: " + order.getShippingAddress().getFullName()));
            document.add(new Paragraph("Address: " + order.getShippingAddress().getAddressLine1()));
            if (order.getShippingAddress().getAddressLine2() != null && !order.getShippingAddress().getAddressLine2().isEmpty()) {
                document.add(new Paragraph("         " + order.getShippingAddress().getAddressLine2()));
            }
            document.add(new Paragraph("City: " + order.getShippingAddress().getCity()));
            document.add(new Paragraph("State: " + order.getShippingAddress().getState()));
            document.add(new Paragraph("Postal Code: " + order.getShippingAddress().getPostalCode()));
            document.add(new Paragraph("Country: " + order.getShippingAddress().getCountry()));
            document.add(new Paragraph("Phone: " + order.getShippingAddress().getPhoneNumber()));
            document.add(new Paragraph("\n"));

            // Add items table header
            document.add(new Paragraph("Order Items:"));
            document.add(new Paragraph("\n"));

            // Add items
            for (Order.OrderItem item : order.getItems()) {
                document.add(new Paragraph(item.getProductName()));
                document.add(new Paragraph("Quantity: " + item.getQuantity()));
                document.add(new Paragraph("Unit Price: $" + item.getUnitPrice()));
                document.add(new Paragraph("Total: $" + item.getTotalPrice()));
                document.add(new Paragraph("\n"));
            }

            // Add totals
            document.add(new Paragraph("Subtotal: $" + order.getSubtotal()));
            document.add(new Paragraph("Tax: $" + order.getTax()));
            document.add(new Paragraph("Shipping: $" + order.getShippingCost()));
            document.add(new Paragraph("Total: $" + order.getTotal()));
            document.add(new Paragraph("\n"));

            // Add payment information
            document.add(new Paragraph("Payment Information:"));
            document.add(new Paragraph("Method: " + order.getPaymentMethod()));
            document.add(new Paragraph("Status: " + order.getPaymentStatus()));

            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Error generating invoice: " + e.getMessage());
        }
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapOrderToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getCustomerOrders() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapOrderToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(String id) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check if user is authorized to view this order
        if (user.getRole().equals("ADMIN") || order.getCustomerId().equals(user.getId())) {
            return mapOrderToResponse(order);
        } else {
            throw new BadRequestException("You are not authorized to view this order");
        }
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get cart
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Calculate totals
        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingCost = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal total = subtotal.add(tax).add(shippingCost);

        // Check if using balance payment
        if ("BALANCE".equals(request.getPaymentMethod())) {
            return checkoutWithBalance(request, customer, cart, subtotal, tax, shippingCost, total);
        }

        // Continue with existing payment logic for other payment methods
        return processRegularCheckout(request, customer, cart, subtotal, tax, shippingCost, total);
    }

    @Transactional
    public OrderResponse checkoutWithBalance(CheckoutRequest request, User customer, Cart cart,
                                             BigDecimal subtotal, BigDecimal tax, BigDecimal shippingCost, BigDecimal total) {
        // Check if user has sufficient balance
        if (balanceService.hasInsufficientBalance(customer.getId(), total)) {
            throw new BadRequestException("Insufficient balance for this order. Available: €" +
                    balanceService.getUserBalance(customer.getId()) + ", Required: €" + total);
        }

        // Process balance payment
        BalanceTransaction transaction = balanceService.processBalancePayment(
                customer.getId(),
                total,
                "Payment pour une commande",
                null // Will be updated with order ID after creation
        );

        // Create order with balance payment
        Order order = createOrderFromCart(cart, request, customer, subtotal, tax, shippingCost, total, "PAID");
        order.setPaymentMethod("BALANCE");
        order.setStripePaymentIntentId(transaction.getId()); // Store transaction ID

        Order savedOrder = orderRepository.save(order);

        // Update transaction with order ID
        transaction.setOrderId(savedOrder.getId());
        balanceTransactionRepository.save(transaction);

        // Clear cart after successful order
        cartService.clearCart();

        // Send order confirmation SMS
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().isEmpty()) {
            String message = String.format(
                    "Your order #%s has been placed and paid with balance. Total: €%.2f. Thank you for shopping with Beauty Center!",
                    savedOrder.getId(),
                    total
            );
            smsService.sendSms(customer.getPhoneNumber(), message);
        }

        // Prepare OrderResponse for email and return
        OrderResponse orderResponse = mapOrderToResponse(savedOrder);

        // Send order confirmation email
        try {
            emailService.sendOrderConfirmationEmail(
                    customer.getEmail(),
                    orderResponse
            );
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        try {
            emailService.sendNewOrderNotificationToAdmin(orderResponse);
        } catch (Exception e) {
            log.error("Failed to send admin notification for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        log.info("Order {} completed with balance payment for customer {}", savedOrder.getId(), customer.getId());
        return orderResponse;
    }

    @Transactional
    protected OrderResponse processRegularCheckout(CheckoutRequest request, User customer, Cart cart,
                                                   BigDecimal subtotal, BigDecimal tax, BigDecimal shippingCost, BigDecimal total) {
        // Verify payment with Stripe if payment method is card
        String paymentStatus = "PENDING";
        if ("STRIPE".equals(request.getPaymentMethod()) || "CREDIT_CARD".equals(request.getPaymentMethod())) {
            if (request.getPaymentIntentId() == null) {
                throw new BadRequestException("Payment intent ID is required for card payments");
            }

            // Verify payment intent was successful
            if (stripeService.isPaymentSucceeded(request.getPaymentIntentId())) {
                paymentStatus = "PAID";
                log.info("Payment verified successfully for payment intent: {}", request.getPaymentIntentId());
            } else {
                log.error("Payment verification failed for payment intent: {}", request.getPaymentIntentId());
                throw new BadRequestException("Payment was not successful. Please try again.");
            }
        } else {
            // For other payment methods (PayPal, Bank Transfer, etc.)
            paymentStatus = "PENDING";
        }

        // Create order with regular payment
        Order order = createOrderFromCart(cart, request, customer, subtotal, tax, shippingCost, total, paymentStatus);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStripePaymentIntentId(request.getPaymentIntentId());
        order.setStripePaymentMethodId(request.getPaymentMethodId());

        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful order
        cartService.clearCart();

        // Send order confirmation SMS
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().isEmpty()) {
            String message = String.format(
                    "Your order #%s has been placed and is being processed. Total: €%.2f. Thank you for shopping with Beauty Center!",
                    savedOrder.getId(),
                    total
            );
            smsService.sendSms(customer.getPhoneNumber(), message);
        }

        // Prepare OrderResponse for email and return
        OrderResponse orderResponse = mapOrderToResponse(savedOrder);

        // Send order confirmation email
        try {
            emailService.sendOrderConfirmationEmail(
                    customer.getEmail(),
                    orderResponse
            );
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        try {
            emailService.sendNewOrderNotificationToAdmin(orderResponse);
        } catch (Exception e) {
            log.error("Failed to send admin notification for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        return orderResponse;
    }

    private Order createOrderFromCart(Cart cart, CheckoutRequest request, User customer,
                                      BigDecimal subtotal, BigDecimal tax, BigDecimal shippingCost,
                                      BigDecimal total, String paymentStatus) {
        // Create order items and update product stock
        List<Order.OrderItem> orderItems = new ArrayList<>();
        for (Cart.CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProductId()));

            // Check stock
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Not enough stock for product: " + product.getName());
            }

            // Update product stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Create order item
            Order.OrderItem orderItem = Order.OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();

            orderItems.add(orderItem);
        }

        // Create shipping address
        Order.ShippingAddress shippingAddress = Order.ShippingAddress.builder()
                .fullName(request.getFullName())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phoneNumber(request.getPhoneNumber())
                .build();

        // Create order
        return Order.builder()
                .customerId(customer.getId())
                .items(orderItems)
                .shippingAddress(shippingAddress)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(shippingCost)
                .total(total)
                .paymentStatus(paymentStatus)
                .orderStatus("PROCESSING")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
    }

    @Transactional
    public OrderResponse updateOrderStatus(String id, String status) {
        // Validate status
        if (!List.of("PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(status)) {
            throw new BadRequestException("Invalid order status");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Only admin can update order status
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getRole().equals("ADMIN")) {
            throw new BadRequestException("You are not authorized to update order status");
        }

        // If cancelling an order, restore product stock and handle refund
        if (status.equals("CANCELLED") && !order.getOrderStatus().equals("CANCELLED")) {
            // Restore product stock
            for (Order.OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getProductId()));

                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            // If payment was made via Stripe, initiate refund
            if (order.getStripePaymentIntentId() != null && "PAID".equals(order.getPaymentStatus())) {
                try {
                    // You would implement refund logic here
                    // For now, we'll just update the payment status
                    order.setPaymentStatus("REFUNDED");
                    log.info("Refund initiated for payment intent: {}", order.getStripePaymentIntentId());
                } catch (Exception e) {
                    log.error("Failed to initiate refund for payment intent {}: {}",
                            order.getStripePaymentIntentId(), e.getMessage());
                    // Continue with cancellation even if refund fails
                }
            }
        }

        order.setOrderStatus(status);
        order.setUpdatedAt(new Date());

        Order updatedOrder = orderRepository.save(order);
        OrderResponse orderResponse = mapOrderToResponse(updatedOrder);

        // Send cancelled order notification to admin
        if (status.equals("CANCELLED")) {
            try {
                emailService.sendCancelledOrderNotificationToAdmin(orderResponse, "Commande annulée par l'administrateur");
            } catch (Exception e) {
                log.error("Failed to send cancelled order notification to admin for order {}: {}",
                        updatedOrder.getId(), e.getMessage());
            }
        }

        // Send order status update SMS
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().isEmpty()) {
            String message = String.format(
                    "Your order #%s has been updated to status: %s. Thank you for shopping with Beauty Center!",
                    updatedOrder.getId(),
                    status
            );
            smsService.sendSms(customer.getPhoneNumber(), message);
        }

        return orderResponse;
    }

    // Helper method to map Order entity to OrderResponse DTO
    private OrderResponse mapOrderToResponse(Order order) {
        List<OrderResponse.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                    return OrderResponse.OrderItemDto.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .imageUrl(product.getImageUrls() != null && !product.getImageUrls().isEmpty() ?
                                    product.getImageUrls().get(0) : null)
                            .build();
                })
                .collect(Collectors.toList());

        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Calculate estimated delivery date (e.g., 5 days from creation)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(order.getCreatedAt());
        calendar.add(Calendar.DAY_OF_YEAR, 5);
        Date estimatedDeliveryDate = calendar.getTime();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getEmail())
                .items(itemDtos)
                .shippingAddress(OrderResponse.ShippingAddressDto.builder()
                        .fullName(order.getShippingAddress().getFullName())
                        .addressLine1(order.getShippingAddress().getAddressLine1())
                        .addressLine2(order.getShippingAddress().getAddressLine2())
                        .city(order.getShippingAddress().getCity())
                        .state(order.getShippingAddress().getState())
                        .postalCode(order.getShippingAddress().getPostalCode())
                        .country(order.getShippingAddress().getCountry())
                        .phoneNumber(order.getShippingAddress().getPhoneNumber())
                        .build())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shippingCost(order.getShippingCost())
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .build();
    }
}