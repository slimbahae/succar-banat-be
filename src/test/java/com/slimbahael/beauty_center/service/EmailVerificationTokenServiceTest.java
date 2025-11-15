package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.model.EmailVerificationToken;
import com.slimbahael.beauty_center.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private RecaptchaService recaptchaService;

    @InjectMocks
    private EmailVerificationTokenService tokenService;

    @Test
    void createPasswordResetTokenPersistsTokenWhenRecaptchaValid() {
        when(recaptchaService.verify("recaptcha-token")).thenReturn(true);
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String generatedToken = tokenService.createPasswordResetToken("user@example.com");

        assertThat(generatedToken).isNotBlank();

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken savedToken = captor.getValue();

        assertThat(savedToken.getEmail()).isEqualTo("user@example.com");
        assertThat(savedToken.getTokenType()).isEqualTo("PASSWORD_RESET");
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getTokenHash()).isNotBlank().isNotEqualTo(generatedToken);
        assertThat(Duration.between(savedToken.getCreatedAt(), savedToken.getExpiresAt()).toMinutes())
                .isBetween(59L, 61L);
    }

    @Test
    void createPasswordResetTokenThrowsWhenRecaptchaFails() {
        when(recaptchaService.verify("bad-token")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> tokenService.createPasswordResetToken("user@example.com"));

        verify(tokenRepository, never()).save(any());
    }
}
