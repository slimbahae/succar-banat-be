package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.AvailabilityRequest;
import com.slimbahael.beauty_center.dto.AvailabilityResponse;
import com.slimbahael.beauty_center.dto.CreateReservationRequest;
import com.slimbahael.beauty_center.dto.ReservationResponse;
import com.slimbahael.beauty_center.service.ReservationService;
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
public class ReservationController {

    private final ReservationService reservationService;

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