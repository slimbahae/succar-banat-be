package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.*;
import com.slimbahael.beauty_center.service.ReservationService;
import com.slimbahael.beauty_center.service.StripeService;
import com.slimbahael.beauty_center.model.Reservation;
import com.slimbahael.beauty_center.repository.ReservationRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final StripeService stripeService;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    // Admin endpoints
    @GetMapping("/api/admin/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    // Staff endpoints
    @GetMapping("/api/staff/reservations")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<ReservationResponse>> getStaffReservations() {
        return ResponseEntity.ok(reservationService.getReservationsByStaff());
    }

    // Customer endpoints
    @GetMapping("/api/customer/reservations")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReservationResponse>> getCustomerReservations() {
        return ResponseEntity.ok(reservationService.getReservationsByCustomer());
    }

    @PostMapping("/api/customer/reservations")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request));
    }

    // Create Stripe Checkout Session for reservation payment
    @PostMapping("/api/customer/reservations/{reservationId}/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CheckoutSessionResponse> createReservationCheckoutSession(
            @PathVariable String reservationId,
            Principal principal) {

        // Get reservation
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        // Verify the reservation belongs to the authenticated user
        User customer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!reservation.getCustomerId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Reservation not found");
        }

        // Create Stripe Checkout Session
        Session session = stripeService.createCheckoutSession(
                reservationId,
                reservation.getTotalAmount(),
                customer.getEmail(),
                "Réservation de service"
        );

        return ResponseEntity.ok(CheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .sessionUrl(session.getUrl())
                .reservationId(reservationId)
                .build());
    }

    // Shared endpoint for specific reservation by ID
    @GetMapping("/api/reservations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable String id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    // Update reservation status (can be called by Admin, Staff, or Customer)
    @PatchMapping("/api/reservations/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, status));
    }

    // Public endpoint for checking availability
    @PostMapping("/api/public/services/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(reservationService.checkAvailability(request));
    }
}