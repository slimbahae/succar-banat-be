package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.AddToCartRequest;
import com.slimbahael.beauty_center.dto.CartResponse;
import com.slimbahael.beauty_center.dto.UpdateCartItemRequest;
import com.slimbahael.beauty_center.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/cart")
@CrossOrigin
@RequiredArgsConstructor
public class CartController {

    // CART FUNCTIONALITY DISABLED - Products available in store only

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> getCart() {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> removeCartItem(@PathVariable String productId) {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearCart() {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }
}