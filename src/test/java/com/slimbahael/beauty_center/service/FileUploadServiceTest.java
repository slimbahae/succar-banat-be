package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private CloudinaryService cloudinaryService;

    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(cloudinaryService);
    }

    @Test
    void uploadProductImageDelegatesToCloudinaryService() throws IOException {
        MockMultipartFile multipartFile = buildImageFile("sample.png", "image/png", 1200, 900);
        String expectedUrl = "https://res.cloudinary.com/demo/image/upload/v123/beauty-center/products/sample.jpg";

        when(cloudinaryService.uploadProductImage(any())).thenReturn(expectedUrl);

        String url = fileUploadService.uploadProductImage(multipartFile);

        assertThat(url).isEqualTo(expectedUrl);
        verify(cloudinaryService, times(1)).uploadProductImage(multipartFile);
    }

    @Test
    void uploadProductImageRejectsInvalidContentType() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThrows(BadRequestException.class, () -> fileUploadService.uploadProductImage(invalidFile));
        verify(cloudinaryService, never()).uploadProductImage(any());
    }

    @Test
    void uploadProductImageRejectsOversizedFile() throws IOException {
        // Create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-image.png",
                "image/png",
                largeContent
        );

        assertThrows(BadRequestException.class, () -> fileUploadService.uploadProductImage(largeFile));
        verify(cloudinaryService, never()).uploadProductImage(any());
    }

    @Test
    void uploadProfileImageDelegatesToCloudinaryService() throws IOException {
        MockMultipartFile multipartFile = buildImageFile("profile.jpg", "image/jpeg", 400, 400);
        String expectedUrl = "https://res.cloudinary.com/demo/image/upload/v123/beauty-center/profiles/profile.jpg";

        when(cloudinaryService.uploadProfileImage(any())).thenReturn(expectedUrl);

        String url = fileUploadService.uploadProfileImage(multipartFile);

        assertThat(url).isEqualTo(expectedUrl);
        verify(cloudinaryService, times(1)).uploadProfileImage(multipartFile);
    }

    @Test
    void uploadServiceImageDelegatesToCloudinaryService() throws IOException {
        MockMultipartFile multipartFile = buildImageFile("service.jpg", "image/jpeg", 600, 400);
        String expectedUrl = "https://res.cloudinary.com/demo/image/upload/v123/beauty-center/services/service.jpg";

        when(cloudinaryService.uploadServiceImage(any())).thenReturn(expectedUrl);

        String url = fileUploadService.uploadServiceImage(multipartFile);

        assertThat(url).isEqualTo(expectedUrl);
        verify(cloudinaryService, times(1)).uploadServiceImage(multipartFile);
    }

    @Test
    void deleteImageDelegatesToCloudinaryService() {
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v123/beauty-center/products/sample.jpg";

        fileUploadService.deleteImage(imageUrl);

        verify(cloudinaryService, times(1)).deleteImage(imageUrl);
    }

    private MockMultipartFile buildImageFile(String name, String contentType, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String format = contentType.equals("image/png") ? "png" : "jpg";
        ImageIO.write(image, format, baos);
        return new MockMultipartFile("file", name, contentType, baos.toByteArray());
    }
}
