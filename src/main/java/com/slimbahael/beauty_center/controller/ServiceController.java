package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.ServiceRequest;
import com.slimbahael.beauty_center.dto.ServiceResponse;
import com.slimbahael.beauty_center.service.BeautyServiceService;
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
public class ServiceController {

    private final BeautyServiceService beautyServiceService;

    // Public endpoints for all users
    @GetMapping("/api/public/services")
    public ResponseEntity<List<ServiceResponse>> getAllActiveServices() {
        return ResponseEntity.ok(beautyServiceService.getActiveServices());
    }

    @GetMapping("/api/public/services/featured")
    public ResponseEntity<List<ServiceResponse>> getFeaturedServices() {
        return ResponseEntity.ok(beautyServiceService.getFeaturedServices());
    }

    @GetMapping("/api/public/services/category/{category}")
    public ResponseEntity<List<ServiceResponse>> getServicesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(beautyServiceService.getServicesByCategory(category));
    }

    @GetMapping("/api/public/services/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable String id) {
        return ResponseEntity.ok(beautyServiceService.getServiceById(id));
    }

    // Admin endpoints for service management
    @GetMapping("/api/admin/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        return ResponseEntity.ok(beautyServiceService.getAllServices());
    }

    @PostMapping("/api/admin/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceRequest serviceRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beautyServiceService.createService(serviceRequest));
    }

    @PutMapping("/api/admin/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequest serviceRequest) {
        return ResponseEntity.ok(beautyServiceService.updateService(id, serviceRequest));
    }

    @DeleteMapping("/api/admin/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        beautyServiceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}