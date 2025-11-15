package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.AddToCartRequest;
import com.slimbahael.beauty_center.dto.CartResponse;
import com.slimbahael.beauty_center.dto.UpdateCartItemRequest;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.Cart;
import com.slimbahael.beauty_center.model.Product;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.CartRepository;
import com.slimbahael.beauty_center.repository.ProductRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartResponse getCart() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find or create cart for user
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElse(Cart.builder()
                        .customerId(customer.getId())
                        .items(new ArrayList<>())
                        .subtotal(BigDecimal.ZERO)
                        .createdAt(new Date())
                        .updatedAt(new Date())
                        .build());

        return mapCartToResponse(cart);
    }

    public CartResponse addToCart(AddToCartRequest request) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new BadRequestException("Product is not available");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Not enough stock available");
        }

        // Find or create cart
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElse(Cart.builder()
                        .customerId(customer.getId())
                        .items(new ArrayList<>())
                        .subtotal(BigDecimal.ZERO)
                        .createdAt(new Date())
                        .updatedAt(new Date())
                        .build());

        // Check if product already exists in cart
        Optional<Cart.CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        BigDecimal unitPrice = getCurrentPrice(product);

        if (existingItemOpt.isPresent()) {
            // Update existing item quantity
            Cart.CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (product.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Not enough stock available");
            }

            existingItem.setQuantity(newQuantity);
            existingItem.setUnitPrice(unitPrice);
            existingItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            // Add new item to cart
            Cart.CartItem newItem = Cart.CartItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(request.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();

            cart.getItems().add(newItem);
        }

        // Update cart subtotal
        updateCartSubtotal(cart);
        cart.setUpdatedAt(new Date());

        Cart savedCart = cartRepository.save(cart);
        return mapCartToResponse(savedCart);
    }

    public CartResponse updateCartItem(String productId, UpdateCartItemRequest request) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get cart
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        // Find item in cart
        Optional<Cart.CartItem> itemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (itemOpt.isEmpty()) {
            throw new ResourceNotFoundException("Product not found in cart");
        }

        Cart.CartItem item = itemOpt.get();

        // If quantity is 0, remove item
        if (request.getQuantity() <= 0) {
            cart.getItems().remove(item);
        } else {
            // Validate stock
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (product.getStockQuantity() < request.getQuantity()) {
                throw new BadRequestException("Not enough stock available");
            }

            BigDecimal unitPrice = getCurrentPrice(product);
            item.setQuantity(request.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())));
        }

        // Update cart subtotal
        updateCartSubtotal(cart);
        cart.setUpdatedAt(new Date());

        Cart savedCart = cartRepository.save(cart);
        return mapCartToResponse(savedCart);
    }

    public void removeCartItem(String productId) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get cart
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        // Find and remove item
        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new ResourceNotFoundException("Product not found in cart");
        }

        // Update cart subtotal
        updateCartSubtotal(cart);
        cart.setUpdatedAt(new Date());

        cartRepository.save(cart);
    }

    public void clearCart() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Delete cart
        cartRepository.deleteByCustomerId(customer.getId());
    }

    // Helper method to update cart subtotal
    private void updateCartSubtotal(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(Cart.CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setSubtotal(subtotal);
    }

    // Helper method to map Cart entity to CartResponse DTO
    private CartResponse mapCartToResponse(Cart cart) {
        List<CartResponse.CartItemDto> itemDtos = cart.getItems().stream()
                .map(item -> {
                    // Get product to get updated info like image and latest price
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
                    BigDecimal unitPrice = getCurrentPrice(product);

                    return CartResponse.CartItemDto.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .totalPrice(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                            .imageUrl(product.getImageUrls() != null && !product.getImageUrls().isEmpty() ?
                                    product.getImageUrls().get(0) : null)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal subtotal = itemDtos.stream()
                .map(CartResponse.CartItemDto::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemDtos)
                .subtotal(subtotal)
                .itemCount(itemDtos.size())
                .build();
    }

    // Helper method to get the current price taking discount into account
    private BigDecimal getCurrentPrice(Product product) {
        if (product.getDiscountPercentage() != null &&
                product.getDiscountStartDate() != null &&
                product.getDiscountEndDate() != null) {

            Date now = new Date();
            if (now.after(product.getDiscountStartDate()) && now.before(product.getDiscountEndDate())) {
                BigDecimal discountAmount = product.getPrice()
                        .multiply(product.getDiscountPercentage())
                        .divide(new BigDecimal("100"));
                return product.getPrice().subtract(discountAmount);
            }
        }
        return product.getPrice();
    }
}