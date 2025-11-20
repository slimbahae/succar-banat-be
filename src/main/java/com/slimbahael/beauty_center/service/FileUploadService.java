// src/main/java/com/slimbahael/beauty_center/service/FileUploadService.java
package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final CloudinaryService cloudinaryService;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadProductImage(MultipartFile file) {
        log.info("Starting product image upload. File: {}, Size: {} bytes, Type: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        validateImageFile(file);

        // Upload to Cloudinary
        return cloudinaryService.uploadProductImage(file);
    }

    public String uploadProfileImage(MultipartFile file) {
        log.info("Starting profile image upload. File: {}", file.getOriginalFilename());
        validateImageFile(file);

        // Upload to Cloudinary
        return cloudinaryService.uploadProfileImage(file);
    }

    public String uploadServiceImage(MultipartFile file) {
        log.info("Starting service image upload. File: {}", file.getOriginalFilename());
        validateImageFile(file);

        // Upload to Cloudinary
        return cloudinaryService.uploadServiceImage(file);
    }

    public void deleteImage(String imageUrl) {
        log.info("Deleting image: {}", imageUrl);
        cloudinaryService.deleteImage(imageUrl);
    }

    private void validateImageFile(MultipartFile file) {
        log.debug("Validating image file...");

        if (file == null || file.isEmpty()) {
            log.warn("Validation failed: File is empty or null");
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Validation failed: File size {} exceeds maximum {}", file.getSize(), MAX_FILE_SIZE);
            throw new BadRequestException("File size too large. Maximum allowed size is 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Validation failed: Invalid content type: {}", contentType);
            throw new BadRequestException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            log.warn("Validation failed: Invalid filename (null or contains ..): {}", originalFilename);
            throw new BadRequestException("Invalid filename");
        }
        if (originalFilename.length() > 255) {
            log.warn("Validation failed: Filename too long ({} characters): {}", originalFilename.length(), originalFilename);
            throw new BadRequestException("Filename too long");
        }
        // Allow more characters in filenames including spaces, parentheses, etc.
        // Only block path traversal and special shell characters
        if (originalFilename.matches(".*[<>:\"|?*\\\\].*")) {
            log.warn("Validation failed: Filename contains forbidden characters: {}", originalFilename);
            throw new BadRequestException("Filename contains forbidden characters: < > : \" | ? * \\");
        }

        // Validate actual content
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                log.warn("Validation failed: No image readers found for file: {}", originalFilename);
                throw new BadRequestException("Uploaded file is not a valid image");
            }
            ImageReader reader = readers.next();
            String formatName = reader.getFormatName().toLowerCase();
            if (!formatName.matches("(jpg|jpeg|png|gif|webp)")) {
                log.warn("Validation failed: Unsupported image format: {} for file: {}", formatName, originalFilename);
                throw new BadRequestException("Unsupported image format: " + formatName);
            }
            log.debug("File validation successful for: {}", originalFilename);
        } catch (IOException e) {
            log.error("Validation failed: Unable to read image content for file: {}", originalFilename, e);
            throw new BadRequestException("Unable to read image content");
        }
    }
}
