package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.JwtAuthResponse;
import com.slimbahael.beauty_center.dto.LoginRequest;
import com.slimbahael.beauty_center.dto.RegisterRequest;
import com.slimbahael.beauty_center.exception.ResourceAlreadyExistsException;
import com.slimbahael.beauty_center.model.EmailVerificationToken;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.slimbahael.beauty_center.security.JwtTokenProvider;
import com.slimbahael.beauty_center.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final InputSanitizationService inputSanitizationService;
    private final PasswordValidationService passwordValidationService;
    private final EmailService emailService;
    private final EmailVerificationTokenService tokenService;
    private final RecaptchaService recaptchaService;
    private final RateLimiterService rateLimiterService;

    // Track failed login attempts per email (or IP if you prefer)
    private final ConcurrentHashMap<String, FailedLoginAttempt> failedAttempts = new ConcurrentHashMap<>();

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    /* ===========================
       AUTH: LOGIN
       =========================== */
    public JwtAuthResponse login(LoginRequest loginRequest) {
        String email = inputSanitizationService.sanitizeEmail(loginRequest.getEmail());
        String password = loginRequest.getPassword();

        if (password == null || password.trim().isEmpty()) {
            throw new BadCredentialsException("Password cannot be empty");
        }

        String attemptKey = email.toLowerCase();
        if (isAccountLocked(attemptKey)) {
            log.warn("Login attempt on locked account: {}", email);
            throw new LockedException("Account temporarily locked due to too many failed attempts. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            if (!user.isEnabled()) {
                throw new DisabledException("Account is disabled. Please verify your email address.");
            }

            clearFailedAttempts(attemptKey);

            String jwt = tokenProvider.generateToken(authentication);
            log.info("Successful login for user: {}", email);

            return new JwtAuthResponse(
                    jwt,
                    "Bearer",
                    user.getRole(),
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail()
            );
        } catch (AuthenticationException e) {
            recordFailedAttempt(attemptKey);
            log.warn("Failed login attempt for email: {} - {}", email, e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /* ===========================
       AUTH: REGISTER
       =========================== */
    public void register(RegisterRequest registerRequest) {
        String firstName = inputSanitizationService.sanitizeAlphanumeric(registerRequest.getFirstName());
        String lastName  = inputSanitizationService.sanitizeAlphanumeric(registerRequest.getLastName());
        String email     = inputSanitizationService.sanitizeEmail(registerRequest.getEmail());
        String password  = registerRequest.getPassword();
        String phone     = registerRequest.getPhoneNumber();
        String role      = registerRequest.getRole();

        PasswordValidationService.PasswordValidationResult pw = passwordValidationService.validatePassword(password);
        if (!pw.isValid()) {
            throw new IllegalArgumentException("Password validation failed: " + pw.getErrorMessage());
        }

        if (phone != null && !phone.trim().isEmpty()) {
            phone = inputSanitizationService.sanitizePhoneNumber(phone);
        }

        if (role == null || role.trim().isEmpty()) {
            role = "CUSTOMER";
        } else {
            role = role.toUpperCase().trim();
            if (!role.equals("CUSTOMER") && !role.equals("STAFF") && !role.equals("ADMIN")) {
                throw new IllegalArgumentException("Invalid role specified");
            }
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration attempt with existing email: {}", email);
            throw new ResourceAlreadyExistsException("Email is already registered");
        }

        try {
            User user = User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .phoneNumber(phone)
                    .role(role)
                    .enabled(false) // enabled after email verification
                    .build();

            userRepository.save(user);

            String verificationToken = tokenService.createEmailVerificationToken(email);
            emailService.sendEmailVerification(email, verificationToken, firstName);

            log.info("New user registered successfully: {} with role: {}", email, role);
        } catch (Exception e) {
            log.error("Failed to register user: {}", email, e);
            throw new RuntimeException("Failed to register user. Please try again.");
        }
    }

    /* ===========================
       EMAIL VERIFICATION
       =========================== */
    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenService.validateToken(token);
        if (tokenOpt.isPresent()) {
            EmailVerificationToken verificationToken = tokenOpt.get();
            if ("EMAIL_VERIFICATION".equals(verificationToken.getTokenType())) {
                User user = userRepository.findByEmail(verificationToken.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                user.setEnabled(true);
                userRepository.save(user);

                tokenService.markTokenAsUsed(token);
                emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

                log.info("Email verified successfully for user: {}", user.getEmail());
                return true;
            }
        }
        return false;
    }

    public void resendVerificationEmail(String email) {
        email = inputSanitizationService.sanitizeEmail(email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            throw new RuntimeException("Email is already verified");
        }

        tokenService.deleteTokensByEmail(email);
        String token = tokenService.createEmailVerificationToken(email);
        emailService.sendEmailVerification(email, token, user.getFirstName());

        log.info("Verification email resent to: {}", email);
    }

    /* ===========================
       PASSWORD RESET (FORGOT)
       =========================== */
    public void initiatePasswordReset(String email, String recaptchaToken) {
        email = inputSanitizationService.sanitizeEmail(email);

        // Rate limit first
        String rateKey = "pwd-reset:" + email.toLowerCase();
        rateLimiterService.ensureAllowed(
                rateKey,
                3,
                Duration.ofMinutes(15),
                "Too many password reset requests. Please wait before trying again."
        );

        // 1) Verify reCAPTCHA BEFORE any state changes
        validateRecaptchaOrThrow(recaptchaToken);

        // 2) Fetch user; continue only if enabled
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.isEnabled()) {

            // 3) Now it’s safe to mutate token state
            tokenService.deleteTokensByEmail(email);

            // (Token service no longer verifies captcha)
            String token = tokenService.createPasswordResetToken(email);

            emailService.sendPasswordReset(email, token, user.getFirstName());
            log.info("Password reset email sent to: {}", email);
        } else {
            log.warn("Password reset requested for non-existent or disabled email: {}", email);
        }
    }

    /* ===========================
       PASSWORD RESET (CONFIRM)
       =========================== */
    public boolean resetPassword(String token, String newPassword) {
        Optional<EmailVerificationToken> tokenOpt = tokenService.validateToken(token);
        if (tokenOpt.isPresent()) {
            EmailVerificationToken resetToken = tokenOpt.get();
            if ("PASSWORD_RESET".equals(resetToken.getTokenType())) {

                PasswordValidationService.PasswordValidationResult pw =
                        passwordValidationService.validatePassword(newPassword);
                if (!pw.isValid()) {
                    throw new IllegalArgumentException("Password validation failed: " + pw.getErrorMessage());
                }

                User user = userRepository.findByEmail(resetToken.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);

                tokenService.markTokenAsUsed(token);
                log.info("Password reset successfully for user: {}", user.getEmail());
                return true;
            }
        }
        return false;
    }

    /* ===========================
       LOGOUT
       =========================== */
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            tokenBlacklistService.blacklistToken(token);
            SecurityContextHolder.clearContext();
            log.info("User logged out successfully");
        }
    }

    /* ===========================
       PASSWORD CHANGE (AUTH’D)
       =========================== */
    public void changePassword(String currentPassword, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        PasswordValidationService.PasswordValidationResult pw =
                passwordValidationService.validatePassword(newPassword);
        if (!pw.isValid()) {
            throw new IllegalArgumentException("New password validation failed: " + pw.getErrorMessage());
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", email);
    }

    /* ===========================
       MISC / LEGACY
       =========================== */
    /** Deprecated – kept for backward compatibility; prefer {@link #initiatePasswordReset} with recaptcha. */
    public void requestPasswordReset(String email) {
        initiatePasswordReset(email, null); // will fail if recaptcha is required; kept intentionally
    }

    /* ===========================
       INTERNAL HELPERS
       =========================== */
    private boolean isAccountLocked(String attemptKey) {
        FailedLoginAttempt attempt = failedAttempts.get(attemptKey);
        if (attempt == null) return false;

        if (attempt.getAttemptCount() >= MAX_LOGIN_ATTEMPTS) {
            LocalDateTime lockoutExpiry = attempt.getLastAttempt().plusMinutes(LOCKOUT_DURATION_MINUTES);
            if (LocalDateTime.now().isBefore(lockoutExpiry)) {
                return true;
            } else {
                failedAttempts.remove(attemptKey);
                return false;
            }
        }
        return false;
    }

    private void recordFailedAttempt(String attemptKey) {
        failedAttempts.compute(attemptKey, (key, attempt) ->
                attempt == null
                        ? new FailedLoginAttempt(1, LocalDateTime.now())
                        : new FailedLoginAttempt(attempt.getAttemptCount() + 1, LocalDateTime.now()));
    }

    private void clearFailedAttempts(String attemptKey) {
        failedAttempts.remove(attemptKey);
    }

    public void cleanupFailedAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        failedAttempts.entrySet().removeIf(e -> e.getValue().getLastAttempt().isBefore(cutoff));
    }

    private void validateRecaptchaOrThrow(String recaptchaToken) {
        if (!recaptchaService.verify(recaptchaToken)) {
            throw new RuntimeException("reCAPTCHA verification failed. Please try again.");
        }
    }

    private static class FailedLoginAttempt {
        private final int attemptCount;
        private final LocalDateTime lastAttempt;
        public FailedLoginAttempt(int attemptCount, LocalDateTime lastAttempt) {
            this.attemptCount = attemptCount;
            this.lastAttempt = lastAttempt;
        }
        public int getAttemptCount() { return attemptCount; }
        public LocalDateTime getLastAttempt() { return lastAttempt; }
    }
}
