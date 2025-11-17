package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.CheckoutRequest;
import com.slimbahael.beauty_center.dto.OrderResponse;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Admin endpoints
    @GetMapping("/api/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    // Customer endpoints - DISABLED (Products available in store only)
    @GetMapping("/api/customer/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getCustomerOrders() {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @GetMapping("/api/customer/orders/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @PostMapping("/api/customer/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }

    @GetMapping("/api/customer/orders/{id}/invoice")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> getInvoice(@PathVariable String id) {
        throw new BadRequestException("Les achats en ligne sont temporairement désactivés. Nos produits sont disponibles en magasin.");
    }


}