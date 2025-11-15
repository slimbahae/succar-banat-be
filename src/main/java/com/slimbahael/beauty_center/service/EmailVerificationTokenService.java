package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.model.EmailVerificationToken;
import com.slimbahael.beauty_center.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final RecaptchaService recaptchaService;

    private static final SecureRandom secureRandom = new SecureRandom();

    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String createEmailVerificationToken(String email) {
        String token = generateSecureToken();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .tokenHash(hashToken(token))
                .email(email)
                .tokenType("EMAIL_VERIFICATION")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);
        log.info("Email verification token created for email: {}", email);

        return token;
    }

    public String createPasswordResetToken(String email) {

        String token = generateSecureToken();

        EmailVerificationToken resetToken = EmailVerificationToken.builder()
                .tokenHash(hashToken(token))
                .email(email)
                .tokenType("PASSWORD_RESET")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        tokenRepository.save(resetToken);
        log.info("Password reset token created for email: {}", email);

        return token;
    }

    public Optional<EmailVerificationToken> validateToken(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByTokenHash(hashToken(token));

        if (tokenOpt.isPresent()) {
            EmailVerificationToken verificationToken = tokenOpt.get();
            if (!verificationToken.isUsed() && !verificationToken.isExpired()) {
                return tokenOpt;
            }
        }

        return Optional.empty();
    }

    @Transactional
    public boolean markTokenAsUsed(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByTokenHash(hashToken(token));

        if (tokenOpt.isPresent()) {
            EmailVerificationToken verificationToken = tokenOpt.get();
            verificationToken.setUsed(true);
            tokenRepository.save(verificationToken);
            log.info("Token marked as used: {}", token);
            return true;
        }

        return false;
    }

    @Transactional
    public void deleteTokensByEmail(String email) {
        tokenRepository.deleteByEmail(email);
        log.info("All tokens deleted for email: {}", email);
    }



    // Add this method to clean up expired tokens properly
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired tokens cleaned up");
    }

    // Add method to check if token exists and is valid
    public boolean isTokenValid(String token) {
        return validateToken(token).isPresent();
    }

    // Add method to get token by email and type
    public Optional<EmailVerificationToken> getTokenByEmailAndType(String email, String tokenType) {
        return tokenRepository.findByEmailAndTokenType(email, tokenType);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
