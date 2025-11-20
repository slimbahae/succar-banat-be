package com.slimbahael.beauty_center.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.slimbahael.beauty_center.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload image to Cloudinary
     * @param file The image file to upload
     * @param folder The folder in Cloudinary to store the image
     * @return The public URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) {
        try {
            log.info("Uploading image to Cloudinary. File: {}, Folder: {}",
                    file.getOriginalFilename(), folder);

            // Upload the file to Cloudinary with transformation options
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "quality", "auto:good",
                            "fetch_format", "auto"
                    ));

            String imageUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Successfully uploaded image to Cloudinary. URL: {}, Public ID: {}",
                    imageUrl, publicId);

            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", file.getOriginalFilename(), e);
            throw new BadRequestException("Failed to upload image to cloud storage: " + e.getMessage());
        }
    }

    /**
     * Upload product image to Cloudinary
     * @param file The product image file
     * @return The public URL of the uploaded image
     */
    public String uploadProductImage(MultipartFile file) {
        return uploadImage(file, "beauty-center/products");
    }

    /**
     * Upload profile image to Cloudinary
     * @param file The profile image file
     * @return The public URL of the uploaded image
     */
    public String uploadProfileImage(MultipartFile file) {
        return uploadImage(file, "beauty-center/profiles");
    }

    /**
     * Upload service image to Cloudinary
     * @param file The service image file
     * @return The public URL of the uploaded image
     */
    public String uploadServiceImage(MultipartFile file) {
        return uploadImage(file, "beauty-center/services");
    }

    /**
     * Delete image from Cloudinary
     * @param imageUrl The public URL of the image to delete
     */
    public void deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                log.warn("Attempted to delete image with null/empty URL");
                return;
            }

            // Extract public_id from Cloudinary URL
            // URL format: https://res.cloudinary.com/cloud_name/image/upload/v123456/folder/filename.jpg
            String publicId = extractPublicIdFromUrl(imageUrl);

            if (publicId == null) {
                log.warn("Could not extract public ID from URL: {}", imageUrl);
                return;
            }

            log.info("Deleting image from Cloudinary. Public ID: {}", publicId);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            if ("ok".equals(resultStatus)) {
                log.info("Successfully deleted image from Cloudinary: {}", publicId);
            } else {
                log.warn("Image deletion returned status: {} for public ID: {}", resultStatus, publicId);
            }

        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary: {}", imageUrl, e);
            // Don't throw exception - deletion failures shouldn't break the app
        }
    }

    /**
     * Extract Cloudinary public ID from image URL
     * @param imageUrl The Cloudinary image URL
     * @return The public ID, or null if extraction fails
     */
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // Example URL: https://res.cloudinary.com/demo/image/upload/v1234567890/beauty-center/products/abc123.jpg
            // Public ID: beauty-center/products/abc123

            if (!imageUrl.contains("cloudinary.com")) {
                return null;
            }

            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            String afterUpload = imageUrl.substring(uploadIndex + 8); // Skip "/upload/"

            // Remove version if present (v1234567890/)
            if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // Remove file extension
            int lastDot = afterUpload.lastIndexOf('.');
            if (lastDot > 0) {
                afterUpload = afterUpload.substring(0, lastDot);
            }

            return afterUpload;

        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}", imageUrl, e);
            return null;
        }
    }
}
