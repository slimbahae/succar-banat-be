package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        ReflectionTestUtils.setField(fileUploadService, "uploadDirectory", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "baseUrl", "http://localhost:8083/api/files");
    }

    @Test
    void uploadProductImageStoresResizedImageAndReturnsUrl() throws IOException {
        MockMultipartFile multipartFile = buildImageFile("sample.png", "image/png", 1200, 900);

        String url = fileUploadService.uploadProductImage(multipartFile);

        assertThat(url).startsWith("http://localhost:8083/api/files/products/");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path savedFile = tempDir.resolve("products").resolve(filename);
        assertThat(Files.exists(savedFile)).isTrue();

        BufferedImage savedImage = ImageIO.read(savedFile.toFile());
        assertThat(savedImage.getWidth()).isLessThanOrEqualTo(800);
        assertThat(savedImage.getHeight()).isLessThanOrEqualTo(600);
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
    }

    private MockMultipartFile buildImageFile(String name, String contentType, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("file", name, contentType, baos.toByteArray());
    }
}
