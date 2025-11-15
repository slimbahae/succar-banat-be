// src/main/java/com/slimbahael/beauty_center/service/FileUploadService.java
package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${file.upload.directory:uploads}")
    private String uploadDirectory;

    @Value("${file.upload.base-url:http://localhost:8083/api/files}")
    private String baseUrl;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadProductImage(MultipartFile file) {
        validateImageFile(file);

        try {
            // Create upload directory if it doesn't exist
            createUploadDirectoryIfNotExists();

            // Generate unique filename
            String fileExtension = getFileExtension(file);
            String uniqueFilename = "product_" + UUID.randomUUID().toString() + "." + fileExtension;

            // Create the file path
            Path uploadPath = Paths.get(uploadDirectory, "products");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Resize and optimize image before saving
            BufferedImage optimizedImage = resizeAndOptimizeImage(file, 800, 600);

            // Save the optimized image
            ImageIO.write(optimizedImage, getImageFormat(fileExtension), filePath.toFile());

            // Return the file URL
            String fileUrl = baseUrl + "/products/" + uniqueFilename;

            log.info("Successfully uploaded product image: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload product image", e);
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    public String uploadProfileImage(MultipartFile file) {
        validateImageFile(file);

        try {
            createUploadDirectoryIfNotExists();

            String fileExtension = getFileExtension(file);
            String uniqueFilename = "profile_" + UUID.randomUUID().toString() + "." + fileExtension;

            Path uploadPath = Paths.get(uploadDirectory, "profiles");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Resize profile image to smaller size (400x400)
            BufferedImage optimizedImage = resizeAndOptimizeImage(file, 400, 400);
            ImageIO.write(optimizedImage, getImageFormat(fileExtension), filePath.toFile());

            String fileUrl = baseUrl + "/profiles/" + uniqueFilename;

            log.info("Successfully uploaded profile image: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload profile image", e);
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    public String uploadServiceImage(MultipartFile file) {
        validateImageFile(file);

        try {
            createUploadDirectoryIfNotExists();

            String fileExtension = getFileExtension(file);
            String uniqueFilename = "service_" + UUID.randomUUID().toString() + "." + fileExtension;

            Path uploadPath = Paths.get(uploadDirectory, "services");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Resize service image
            BufferedImage optimizedImage = resizeAndOptimizeImage(file, 600, 400);
            ImageIO.write(optimizedImage, getImageFormat(fileExtension), filePath.toFile());

            String fileUrl = baseUrl + "/services/" + uniqueFilename;

            log.info("Successfully uploaded service image: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload service image", e);
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    public void deleteImage(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.startsWith(baseUrl)) {
                String relativePath = imageUrl.substring(baseUrl.length());
                Path filePath = Paths.get(uploadDirectory + relativePath);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Successfully deleted image: {}", imageUrl);
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete image: {}", imageUrl, e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size too large. Maximum allowed size is 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename");
        }
        if (originalFilename.length() > 255) {
            throw new BadRequestException("Filename too long");
        }
        if (!originalFilename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new BadRequestException("Filename contains forbidden characters");
        }

        // Validate actual content
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new BadRequestException("Uploaded file is not a valid image");
            }
            ImageReader reader = readers.next();
            String formatName = reader.getFormatName().toLowerCase();
            if (!formatName.matches("(jpg|jpeg|png|gif|webp)")) {
                throw new BadRequestException("Unsupported image format: " + formatName);
            }
        } catch (IOException e) {
            throw new BadRequestException("Unable to read image content");
        }
    }

    private BufferedImage resizeAndOptimizeImage(MultipartFile file, int maxWidth, int maxHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        if (originalImage == null) {
            throw new BadRequestException("Invalid image file");
        }

        // Calculate new dimensions while maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // If image is smaller than max dimensions, don't resize
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return originalImage;
        }

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        // Align extension with actual content type
        if (file.getContentType() != null) {
            switch (file.getContentType().toLowerCase()) {
                case "image/png":
                    return "png";
                case "image/gif":
                    return "gif";
                case "image/webp":
                    return "webp";
                default:
                    return extension;
            }
        }
        return extension;
    }

    private String getImageFormat(String fileExtension) {
        String normalized = fileExtension.toLowerCase();
        if ("webp".equals(normalized)) {
            return "jpg";
        }
        return normalized;
    }
}
