package com.slimbahael.beauty_center.controller;

import com.slimbahael.beauty_center.dto.*;
import com.slimbahael.beauty_center.service.AuthService;
import com.slimbahael.beauty_center.service.EmailVerificationTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
@Tag(name = "Authentication", description = "Authentication and email verification endpoints")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationTokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Registration successful. Please check your email to verify your account.",
                "A verification email has been sent to " + registerRequest.getEmail()
        ));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address", description = "Verify user email address using verification token")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            boolean isVerified = authService.verifyEmail(request.getToken());

            if (isVerified) {
                return ResponseEntity.ok(new ApiResponse<>(
                        true,
                        "Email verified successfully",
                        "Your email has been verified. You can now log in."
                ));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Invalid or expired verification token",
                        null
                ));
            }
        } catch (Exception e) {
            log.error("Error verifying email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    "Failed to verify email: " + e.getMessage(),
                    null
            ));
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Resend verification email to user")
    public ResponseEntity<ApiResponse<String>> resendVerification(@RequestParam String email) {
        try {
            authService.resendVerificationEmail(email);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Verification email sent successfully",
                    "Please check your email for the verification link."
            ));
        } catch (Exception e) {
            log.error("Error resending verification email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    "Failed to resend verification email: " + e.getMessage(),
                    null
            ));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send password reset email to user")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.initiatePasswordReset(request.getEmail(), request.getRecaptchaToken());
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Password reset email sent successfully",
                    "If an account with this email exists, you will receive a password reset link."
            ));
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    "Failed to send password reset email: " + e.getMessage(),
                    null
            ));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset user password using reset token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            boolean isReset = authService.resetPassword(request.getToken(), request.getNewPassword());

            if (isReset) {
                return ResponseEntity.ok(new ApiResponse<>(
                        true,
                        "Password reset successfully",
                        "Your password has been updated. Please log in with your new password."
                ));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Invalid or expired reset token",
                        null
                ));
            }
        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    "Failed to reset password: " + e.getMessage(),
                    null
            ));
        }
    }

    @GetMapping("/verify-token")
    @Operation(summary = "Verify token validity", description = "Check if a verification or reset token is valid")
    public ResponseEntity<ApiResponse<Boolean>> verifyToken(@RequestParam String token) {
        try {
            boolean isValid = tokenService.isTokenValid(token);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    isValid ? "Token is valid" : "Token is invalid or expired",
                    isValid
            ));
        } catch (Exception e) {
            log.error("Error verifying token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    "Failed to verify token: " + e.getMessage(),
                    false
            ));
        }
    }
}