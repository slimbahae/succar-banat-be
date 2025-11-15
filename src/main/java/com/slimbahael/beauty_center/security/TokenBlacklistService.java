package com.slimbahael.beauty_center.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.put(token, LocalDateTime.now());
            log.info("Token blacklisted successfully");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return token != null && blacklistedTokens.containsKey(token);
    }

    public void removeToken(String token) {
        if (token != null) {
            blacklistedTokens.remove(token);
        }
    }

    // Clean up expired tokens every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // Remove tokens older than 24 hours

        int beforeSize = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        int afterSize = blacklistedTokens.size();

        if (beforeSize != afterSize) {
            log.info("Cleaned up {} expired blacklisted tokens", beforeSize - afterSize);
        }
    }

    public int getBlacklistedTokenCount() {
        return blacklistedTokens.size();
    }
}