// src/main/java/com/slimbahael/beauty_center/controller/FileController.java
package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload/product-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {

        try {
            String imageUrl = fileUploadService.uploadProductImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Product image uploaded successfully");
            response.put("filename", extractFilename(imageUrl));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload product image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/upload/profile-image")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {

        try {
            String imageUrl = fileUploadService.uploadProfileImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Profile image uploaded successfully");
            response.put("filename", extractFilename(imageUrl));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload profile image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/upload/service-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadServiceImage(
            @RequestParam("file") MultipartFile file) {

        try {
            String imageUrl = fileUploadService.uploadServiceImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Service image uploaded successfully");
            response.put("filename", extractFilename(imageUrl));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload service image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // Note: These endpoints are deprecated since we now use Cloudinary CDN
    // Images are served directly from Cloudinary URLs, not through our backend
    // Keeping these for backward compatibility with old local URLs (will return 410 Gone)

    @GetMapping("/products/{filename:.+}")
    public ResponseEntity<Map<String, String>> getProductImage(@PathVariable String filename) {
        log.warn("Deprecated endpoint called: /products/{}. Images should be accessed via Cloudinary CDN", filename);
        Map<String, String> response = new HashMap<>();
        response.put("message", "This endpoint is deprecated. Images are now served from Cloudinary CDN.");
        response.put("filename", filename);
        return ResponseEntity.status(HttpStatus.GONE).body(response); // 410 Gone
    }

    @GetMapping("/profiles/{filename:.+}")
    public ResponseEntity<Map<String, String>> getProfileImage(@PathVariable String filename) {
        log.warn("Deprecated endpoint called: /profiles/{}. Images should be accessed via Cloudinary CDN", filename);
        Map<String, String> response = new HashMap<>();
        response.put("message", "This endpoint is deprecated. Images are now served from Cloudinary CDN.");
        response.put("filename", filename);
        return ResponseEntity.status(HttpStatus.GONE).body(response);
    }

    @GetMapping("/services/{filename:.+}")
    public ResponseEntity<Map<String, String>> getServiceImage(@PathVariable String filename) {
        log.warn("Deprecated endpoint called: /services/{}. Images should be accessed via Cloudinary CDN", filename);
        Map<String, String> response = new HashMap<>();
        response.put("message", "This endpoint is deprecated. Images are now served from Cloudinary CDN.");
        response.put("filename", filename);
        return ResponseEntity.status(HttpStatus.GONE).body(response);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteImage(@RequestParam String imageUrl) {
        try {
            fileUploadService.deleteImage(imageUrl);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Image deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // Health check endpoint for file upload system
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getUploadSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Since we're using Cloudinary, we just check if the service is configured
            health.put("status", "UP");
            health.put("storage", "cloudinary");
            health.put("message", "Using Cloudinary cloud storage");

            // Note: We could ping Cloudinary API here, but it's not necessary
            // The upload will fail with clear error if Cloudinary is misconfigured

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    private String extractFilename(String url) {
        if (url == null) return null;
        return url.substring(url.lastIndexOf('/') + 1);
    }
}