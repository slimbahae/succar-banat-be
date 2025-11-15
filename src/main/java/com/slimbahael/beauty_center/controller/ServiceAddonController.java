package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.ServiceAddonRequest;
import com.slimbahael.beauty_center.dto.ServiceAddonResponse;
import com.slimbahael.beauty_center.service.ServiceAddonService;
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
public class ServiceAddonController {

    private final ServiceAddonService serviceAddonService;

    // Public endpoints for all users
    @GetMapping("/api/public/service-addons")
    public ResponseEntity<List<ServiceAddonResponse>> getAllActiveServiceAddons() {
        return ResponseEntity.ok(serviceAddonService.getActiveServiceAddons());
    }

    @GetMapping("/api/public/service-addons/service/{serviceId}")
    public ResponseEntity<List<ServiceAddonResponse>> getServiceAddonsByServiceId(@PathVariable String serviceId) {
        return ResponseEntity.ok(serviceAddonService.getServiceAddonsByServiceId(serviceId));
    }

    @GetMapping("/api/public/service-addons/{id}")
    public ResponseEntity<ServiceAddonResponse> getServiceAddonById(@PathVariable String id) {
        return ResponseEntity.ok(serviceAddonService.getServiceAddonById(id));
    }

    // Admin endpoints for service addon management
    @GetMapping("/api/admin/service-addons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceAddonResponse>> getAllServiceAddons() {
        return ResponseEntity.ok(serviceAddonService.getAllServiceAddons());
    }

    @PostMapping("/api/admin/service-addons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceAddonResponse> createServiceAddon(@Valid @RequestBody ServiceAddonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceAddonService.createServiceAddon(request));
    }

    @PutMapping("/api/admin/service-addons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceAddonResponse> updateServiceAddon(
            @PathVariable String id,
            @Valid @RequestBody ServiceAddonRequest request) {
        return ResponseEntity.ok(serviceAddonService.updateServiceAddon(id, request));
    }

    @DeleteMapping("/api/admin/service-addons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteServiceAddon(@PathVariable String id) {
        serviceAddonService.deleteServiceAddon(id);
        return ResponseEntity.noContent().build();
    }
}