package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.EmailVerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends MongoRepository<EmailVerificationToken, String> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    Optional<EmailVerificationToken> findByEmailAndTokenType(String email, String tokenType);
}
