package com.slimbahael.beauty_center.scheduler;

import com.slimbahael.beauty_center.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.reviews.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewScheduler {

    private final ReviewService reviewService;

    // Run every Sunday at 2 AM
    @Scheduled(cron = "0 0 2 * * SUN")
    public void fetchReviewsWeekly() {
        log.info("Starting weekly review fetch job...");

        try {
            reviewService.fetchAndStoreReviews();
            log.info("Weekly review fetch job completed successfully");
        } catch (Exception e) {
            log.error("Weekly review fetch job failed: {}", e.getMessage(), e);
        }
    }

    // Manual trigger for testing - runs every 5 minutes (remove in production)
    @Scheduled(fixedRate = 300000) // 5 minutes
    @ConditionalOnProperty(name = "scheduler.reviews.test.enabled", havingValue = "true")
    public void fetchReviewsForTesting() {
        log.info("Starting test review fetch job...");

        try {
            reviewService.fetchAndStoreReviews();
            log.info("Test review fetch job completed successfully");
        } catch (Exception e) {
            log.error("Test review fetch job failed: {}", e.getMessage(), e);
        }
    }
}