// src/main/java/com/slimbahael/beauty_center/controller/FileController.java
package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.service.FileUploadService;
// Alternative: import com.slimbahael.beauty_center.service.SimpleFileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileUploadService fileUploadService;
    // Alternative: private final SimpleFileUploadService fileUploadService;

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

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

    @GetMapping("/products/{filename:.+}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String filename) {
        return serveFile("products", filename);
    }

    @GetMapping("/profiles/{filename:.+}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String filename) {
        return serveFile("profiles", filename);
    }

    @GetMapping("/services/{filename:.+}")
    public ResponseEntity<Resource> getServiceImage(@PathVariable String filename) {
        return serveFile("services", filename);
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
            Path uploadPath = Paths.get(uploadDirectory);
            boolean directoryExists = Files.exists(uploadPath);
            boolean directoryWritable = Files.isWritable(uploadPath);

            health.put("status", directoryExists && directoryWritable ? "UP" : "DOWN");
            health.put("uploadDirectory", uploadPath.toAbsolutePath().toString());
            health.put("directoryExists", directoryExists);
            health.put("directoryWritable", directoryWritable);

            // Check subdirectories
            Map<String, Boolean> subdirectories = new HashMap<>();
            subdirectories.put("products", Files.exists(uploadPath.resolve("products")));
            subdirectories.put("profiles", Files.exists(uploadPath.resolve("profiles")));
            subdirectories.put("services", Files.exists(uploadPath.resolve("services")));
            health.put("subdirectories", subdirectories);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    private ResponseEntity<Resource> serveFile(String folder, String filename) {
        try {
            Path filePath = Paths.get(uploadDirectory, folder, filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=86400") // Cache for 1 day
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                log.warn("File not found or not readable: {}/{}", folder, filename);
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}/{}", folder, filename, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error while serving file: {}/{}", folder, filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String extractFilename(String url) {
        if (url == null) return null;
        return url.substring(url.lastIndexOf('/') + 1);
    }
}