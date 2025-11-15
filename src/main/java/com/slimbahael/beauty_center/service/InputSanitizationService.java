// src/main/java/com/slimbahael/beauty_center/service/InputSanitizationService.java
package com.slimbahael.beauty_center.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@Slf4j
public class InputSanitizationService {

    // Patterns for various validation scenarios
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT_TAGS = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('|(\\-\\-)|(;)|(\\||\\|)|(\\*|\\*))", Pattern.CASE_INSENSITIVE);
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(javascript:|vbscript:|onload|onerror|onclick|onmouseover)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9\\s\\-\\(\\)]{10,15}$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\s\\-_.,!?]+$");

    /**
     * Sanitize general text input by removing potentially dangerous content
     */
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Remove script tags first
        sanitized = SCRIPT_TAGS.matcher(sanitized).replaceAll("");

        // Remove HTML tags
        sanitized = HTML_TAGS.matcher(sanitized).replaceAll("");

        // Remove null bytes
        sanitized = sanitized.replace("\0", "");

        // Limit length
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
            log.warn("Input truncated due to length: original length was {}", input.length());
        }

        return sanitized;
    }

    /**
     * Sanitize text specifically for database storage
     */
    public String sanitizeForDatabase(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = sanitizeString(input);

        // Additional checks for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            log.warn("Potential SQL injection attempt detected and blocked");
            throw new IllegalArgumentException("Input contains potentially dangerous characters");
        }

        return sanitized;
    }

    /**
     * Sanitize HTML content (for descriptions, etc.)
     */
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Remove script tags
        sanitized = SCRIPT_TAGS.matcher(sanitized).replaceAll("");

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(sanitized).find()) {
            log.warn("Potential XSS attempt detected and blocked");
            throw new IllegalArgumentException("Input contains potentially dangerous script content");
        }

        // Allow only safe HTML tags (basic formatting)
        sanitized = sanitized.replaceAll("<(?!/?(?:b|i|u|strong|em|p|br|ul|ol|li)\\b)[^>]*>", "");

        return sanitized;
    }

    /**
     * Validate and sanitize email addresses
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String sanitized = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Additional email-specific sanitization
        if (sanitized.length() > 254) { // RFC 5321 limit
            throw new IllegalArgumentException("Email address too long");
        }

        return sanitized;
    }

    /**
     * Validate and sanitize phone numbers
     */
    public String sanitizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        String sanitized = phone.trim();

        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        return sanitized;
    }

    /**
     * Sanitize alphanumeric input (names, etc.)
     */
    public String sanitizeAlphanumeric(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        if (!ALPHANUMERIC_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Input contains invalid characters");
        }

        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Input too long");
        }

        return sanitized;
    }

    /**
     * Sanitize file paths to prevent directory traversal
     */
    public String sanitizeFilePath(String path) {
        if (path == null) {
            return null;
        }

        String sanitized = path.trim();

        // Check for directory traversal attempts
        if (sanitized.contains("..") || sanitized.contains("/") || sanitized.contains("\\")) {
            throw new IllegalArgumentException("Invalid file path: directory traversal detected");
        }

        // Check for null bytes
        if (sanitized.contains("\0")) {
            throw new IllegalArgumentException("Invalid file path: null byte detected");
        }

        // Limit length
        if (sanitized.length() > 255) {
            throw new IllegalArgumentException("File path too long");
        }

        return sanitized;
    }

    /**
     * Sanitize URL input
     */
    public String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String sanitized = url.trim();

        // Check for dangerous protocols
        if (sanitized.toLowerCase().startsWith("javascript:") ||
                sanitized.toLowerCase().startsWith("vbscript:") ||
                sanitized.toLowerCase().startsWith("data:")) {
            throw new IllegalArgumentException("Unsafe URL protocol detected");
        }

        // Ensure it starts with http or https
        if (!sanitized.toLowerCase().startsWith("http://") &&
                !sanitized.toLowerCase().startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }

        return sanitized;
    }

    /**
     * Check if input contains potential security threats
     */
    public boolean containsSecurityThreats(String input) {
        if (input == null) {
            return false;
        }

        return SCRIPT_TAGS.matcher(input).find() ||
                XSS_PATTERN.matcher(input).find() ||
                SQL_INJECTION_PATTERN.matcher(input).find() ||
                input.contains("..") ||
                input.contains("\0");
    }

    /**
     * Sanitize search query input
     */
    public String sanitizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }

        String sanitized = query.trim();

        // Remove HTML tags
        sanitized = HTML_TAGS.matcher(sanitized).replaceAll("");

        // Remove special characters that could be used for injection
        sanitized = sanitized.replaceAll("[<>\"'%;()&+]", "");

        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }
}