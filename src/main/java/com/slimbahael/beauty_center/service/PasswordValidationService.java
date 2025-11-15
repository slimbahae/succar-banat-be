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

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    // Common weak passwords to reject
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "password", "123456", "password123", "admin", "qwerty", "letmein",
            "welcome", "monkey", "dragon", "master", "password1", "123456789",
            "12345678", "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"
    );

    // Patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s.*");

    /**
     * Validates password strength according to security best practices
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

        // Check for uppercase letters
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        // Check for lowercase letters
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        // Check for digits
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one digit");
        }

        // Check for special characters
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\"\\,.<>/?)");
        }

        // Check for whitespace (not allowed)
        if (WHITESPACE_PATTERN.matcher(password).matches()) {
            errors.add("Password must not contain whitespace characters");
        }

        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common, please choose a more secure password");
        }

        // Check for sequential characters
        if (hasSequentialCharacters(password)) {
            errors.add("Password must not contain sequential characters (e.g., 123, abc)");
        }

        // Check for repeated characters
        if (hasRepeatedCharacters(password)) {
            errors.add("Password must not contain more than 2 consecutive identical characters");
        }

        boolean isValid = errors.isEmpty();
        return new PasswordValidationResult(isValid, errors);
    }

    /**
     * Checks if password contains sequential characters
     */
    private boolean hasSequentialCharacters(String password) {
        String lowerPassword = password.toLowerCase();

        // Check for sequential alphabetic characters (3 or more)
        for (int i = 0; i < lowerPassword.length() - 2; i++) {
            char c1 = lowerPassword.charAt(i);
            char c2 = lowerPassword.charAt(i + 1);
            char c3 = lowerPassword.charAt(i + 2);

            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true; // Sequential ascending
                }
                if (c2 == c1 - 1 && c3 == c2 - 1) {
                    return true; // Sequential descending
                }
            }
        }

        // Check for sequential numeric characters (3 or more)
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true; // Sequential ascending
                }
                if (c2 == c1 - 1 && c3 == c2 - 1) {
                    return true; // Sequential descending
                }
            }
        }

        return false;
    }

    /**
     * Checks if password has more than 2 consecutive identical characters
     */
    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                    password.charAt(i + 1) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates password strength score (0-100)
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        // Character type scoring
        if (UPPERCASE_PATTERN.matcher(password).matches()) score += 15;
        if (LOWERCASE_PATTERN.matcher(password).matches()) score += 15;
        if (DIGIT_PATTERN.matcher(password).matches()) score += 15;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score += 20;

        // Penalty for common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) score -= 50;

        // Penalty for sequential or repeated characters
        if (hasSequentialCharacters(password)) score -= 20;
        if (hasRepeatedCharacters(password)) score -= 15;

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