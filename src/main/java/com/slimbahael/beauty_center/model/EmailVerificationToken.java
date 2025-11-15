package com.slimbahael.beauty_center.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    private String email;

    private String tokenType; // "EMAIL_VERIFICATION" or "PASSWORD_RESET"

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private boolean used;

    // Token expires after 24 hours for email verification, 1 hour for password reset
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
