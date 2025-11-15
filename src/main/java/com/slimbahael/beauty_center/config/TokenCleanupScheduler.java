package com.slimbahael.beauty_center.config;

import com.slimbahael.beauty_center.service.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final EmailVerificationTokenService tokenService;

    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired tokens");
        tokenService.cleanupExpiredTokens();
        log.info("Completed scheduled cleanup of expired tokens");
    }
}