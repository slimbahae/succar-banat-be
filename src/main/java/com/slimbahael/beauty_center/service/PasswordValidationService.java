// src/main/java/com/slimbahael/beauty_center/service/PasswordValidationService.java
package com.slimbahael.beauty_center.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PasswordValidationService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;

    // Common weak passwords to reject
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "password", "123456", "password123", "admin", "qwerty", "letmein",
            "welcome", "monkey", "dragon", "master", "password1", "123456789",
            "12345678", "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"
    );

    // Patterns for password validation (simplified)
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s.*");

    /**
     * Validates password strength (simplified for better user experience)
     */
    public PasswordValidationResult validatePassword(String password) {
        if (password == null) {
            return new PasswordValidationResult(false, List.of("Password cannot be null"));
        }

        List<String> errors = new ArrayList<>();

        // Check length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            errors.add("Password must not exceed " + MAX_PASSWORD_LENGTH + " characters");
        }

        // Check for whitespace (not allowed)
        if (WHITESPACE_PATTERN.matcher(password).matches()) {
            errors.add("Password must not contain whitespace characters");
        }

        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common, please choose a more secure password");
        }

        boolean isValid = errors.isEmpty();
        return new PasswordValidationResult(isValid, errors);
    }

    /**
     * Generates password strength score (0-100) - simplified
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 6) score += 40;
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 20;
        if (password.length() >= 16) score += 20;

        // Penalty for common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) score -= 50;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Gets password strength description
     */
    public String getPasswordStrengthDescription(int score) {
        if (score < 30) return "Very Weak";
        if (score < 50) return "Weak";
        if (score < 70) return "Fair";
        if (score < 85) return "Good";
        return "Strong";
    }

    /**
     * Result class for password validation
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}