// src/main/java/com/slimbahael/beauty_center/security/RateLimitingFilter.java
package com.slimbahael.beauty_center.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitingFilter implements Filter {

    private final ConcurrentHashMap<String, RequestTracker> requestTrackers = new ConcurrentHashMap<>();

    // Rate limits per endpoint type
    private static final int LOGIN_REQUESTS_PER_MINUTE = 5;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 100;
    private static final int UPLOAD_REQUESTS_PER_MINUTE = 10;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientId = getClientIdentifier(httpRequest);
        String endpoint = httpRequest.getRequestURI();

        // Determine rate limit based on endpoint
        int allowedRequests = determineRateLimit(endpoint);

        if (isRateLimited(clientId, allowedRequests)) {
            log.warn("Rate limit exceeded for client: {} on endpoint: {}", clientId, endpoint);

            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get real IP address (considering proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return remoteAddr;
    }

    private int determineRateLimit(String endpoint) {
        if (endpoint.contains("/api/auth/login")) {
            return LOGIN_REQUESTS_PER_MINUTE;
        } else if (endpoint.contains("/api/files/upload")) {
            return UPLOAD_REQUESTS_PER_MINUTE;
        } else {
            return GENERAL_REQUESTS_PER_MINUTE;
        }
    }

    private boolean isRateLimited(String clientId, int allowedRequests) {
        LocalDateTime now = LocalDateTime.now();

        requestTrackers.compute(clientId, (key, tracker) -> {
            if (tracker == null || ChronoUnit.MINUTES.between(tracker.windowStart, now) >= 1) {
                // Create new tracker or reset if window expired
                return new RequestTracker(now, new AtomicInteger(1));
            } else {
                // Increment counter in current window
                tracker.requestCount.incrementAndGet();
                return tracker;
            }
        });

        RequestTracker tracker = requestTrackers.get(clientId);
        return tracker.requestCount.get() > allowedRequests;
    }

    // Cleanup old entries periodically
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minus(2, ChronoUnit.MINUTES);
        requestTrackers.entrySet().removeIf(entry ->
                entry.getValue().windowStart.isBefore(cutoff));
    }

    private static class RequestTracker {
        final LocalDateTime windowStart;
        final AtomicInteger requestCount;

        RequestTracker(LocalDateTime windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}