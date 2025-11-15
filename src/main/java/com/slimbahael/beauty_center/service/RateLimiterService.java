package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    public void ensureAllowed(String key, int maxRequests, Duration window, String message) {
        Instant now = Instant.now();
        Deque<Instant> deque = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            Instant cutoff = now.minus(window);
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                throw new BadRequestException(message);
            }
            deque.offerLast(now);
        }
    }
}
