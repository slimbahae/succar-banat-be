package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.SerpApiResponse;
import com.slimbahael.beauty_center.model.Review;
import com.slimbahael.beauty_center.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;

    @Value("${serpapi.api.key}")
    private String serpApiKey;

    @Value("${serpapi.google.maps.data.id}")
    private String googleMapsDataId;

    private static final String SERPAPI_URL = "https://serpapi.com/search.json";
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d+ (day|week|month|year)s? ago");

    public List<Review> fetchAndStoreReviews() {
        log.info("Starting to fetch reviews from SerpAPI with pagination...");

        List<Review> newReviews = new ArrayList<>();
        String nextPageToken = null;
        int page = 1;

        try {
            while (true) {
                String url = buildSerpApiUrl(nextPageToken);
                log.info("Fetching page {} from SerpAPI: {}", page, url);

                SerpApiResponse response = restTemplate.getForObject(url, SerpApiResponse.class);

                if (response == null || response.getReviews() == null || response.getReviews().isEmpty()) {
                    log.info("No more reviews found. Ending fetch.");
                    break;
                }

                log.info("Fetched {} reviews on page {}", response.getReviews().size(), page);

                for (SerpApiResponse.GoogleReviewDto reviewDto : response.getReviews()) {
                    try {
                        Review review = convertToReview(reviewDto);
                        Optional<Review> existingReview = reviewRepository.findByReviewId(review.getReviewId());

                        if (existingReview.isEmpty()) {
                            review.setCreatedAt(LocalDateTime.now());
                            review.setUpdatedAt(LocalDateTime.now());
                            review.setActive(true);
                            review.setSource("GOOGLE_MAPS");

                            Review savedReview = reviewRepository.save(review);
                            newReviews.add(savedReview);
                            log.info("Saved new review from user: {}", review.getUserName());
                        } else {
                            Review existing = existingReview.get();
                            if (updateReviewIfChanged(existing, review)) {
                                existing.setUpdatedAt(LocalDateTime.now());
                                reviewRepository.save(existing);
                                log.info("Updated existing review from user: {}", existing.getUserName());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing review: {}", e.getMessage(), e);
                    }
                }

                // Check for pagination
                if (response.getPagination() != null &&
                        response.getPagination().getNextPageToken() != null &&
                        !response.getPagination().getNextPageToken().isEmpty()) {

                    nextPageToken = response.getPagination().getNextPageToken();
                    page++;

                    // Optional: wait 2 seconds to avoid rate limits
                    Thread.sleep(2000);

                } else {
                    log.info("No next page token found. Completed all pages.");
                    break;
                }
            }

        } catch (HttpClientErrorException e) {
            log.error("HTTP error from SerpAPI: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("SerpAPI returned error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching reviews from SerpAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reviews from SerpAPI", e);
        }

        log.info("Total new reviews processed: {}", newReviews.size());
        return newReviews;
    }



    private String buildSerpApiUrl(String nextPageToken) {
        if (nextPageToken != null && !nextPageToken.isEmpty()) {
            // pages suivantes : on peut inclure next_page_token et num
            return String.format("%s?engine=google_maps_reviews&data_id=%s&hl=en&num=20&api_key=%s&next_page_token=%s",
                    SERPAPI_URL, googleMapsDataId, serpApiKey, nextPageToken);
        } else {
            // page initiale : pas de paramètre `num`
            return String.format("%s?engine=google_maps_reviews&data_id=%s&hl=en&api_key=%s",
                    SERPAPI_URL, googleMapsDataId, serpApiKey);
        }
    }




    private Review convertToReview(SerpApiResponse.GoogleReviewDto reviewDto) {
        Review review = Review.builder()
                .reviewId(generateReviewId(reviewDto))
                .userName(reviewDto.getUser() != null ? reviewDto.getUser().getName() : "Anonymous")
                .userProfileImage(reviewDto.getUser() != null ? reviewDto.getUser().getThumbnail() : null)
                .userLocalGuideInfo(reviewDto.getUser() != null && reviewDto.getUser().getLocalGuide() != null
                        ? reviewDto.getUser().getLocalGuide().toString() : null)
                .rating(reviewDto.getRating())
                .reviewText(reviewDto.getSnippet() != null ? reviewDto.getSnippet() : "")
                .reviewDate(parseReviewDate(reviewDto.getDate()))
                .likesCount(reviewDto.getLikes() != null ? reviewDto.getLikes() : 0)
                .reviewImages(reviewDto.getImages() != null ? reviewDto.getImages() : new ArrayList<>())
                .build();

        // Add owner response if available
        if (reviewDto.getResponse() != null) {
            review.setOwnerResponse(reviewDto.getResponse().getSnippet());
            review.setOwnerResponseDate(parseReviewDate(reviewDto.getResponse().getDate()));
        }

        return review;
    }

    private String generateReviewId(SerpApiResponse.GoogleReviewDto reviewDto) {
        // Generate a unique ID based on user name, date, and rating
        String userName = reviewDto.getUser() != null ? reviewDto.getUser().getName() : "anonymous";
        String userLink = reviewDto.getUser() != null ? reviewDto.getUser().getLink() : "";
        String date = reviewDto.getDate() != null ? reviewDto.getDate() : "unknown";

        return String.format("%s_%s_%d_%s",
                        userName,
                        date,
                        reviewDto.getRating(),
                        userLink.hashCode())
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .toLowerCase();
    }

    private LocalDateTime parseReviewDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // Handle relative dates like "2 days ago", "1 week ago", etc.
            if (DATE_PATTERN.matcher(dateString.toLowerCase()).matches()) {
                return parseRelativeDate(dateString);
            }

            // Try to parse as ISO date
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        } catch (DateTimeParseException e) {
            log.warn("Unable to parse date: {}, using current time", dateString);
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseRelativeDate(String dateString) {
        LocalDateTime now = LocalDateTime.now();
        String[] parts = dateString.toLowerCase().split(" ");

        if (parts.length >= 3) {
            try {
                int amount = Integer.parseInt(parts[0]);
                String unit = parts[1];

                switch (unit) {
                    case "day":
                    case "days":
                        return now.minusDays(amount);
                    case "week":
                    case "weeks":
                        return now.minusWeeks(amount);
                    case "month":
                    case "months":
                        return now.minusMonths(amount);
                    case "year":
                    case "years":
                        return now.minusYears(amount);
                    default:
                        return now;
                }
            } catch (NumberFormatException e) {
                log.warn("Unable to parse relative date amount: {}", dateString);
                return now;
            }
        }

        return now;
    }

    private boolean updateReviewIfChanged(Review existing, Review newReview) {
        boolean hasChanges = false;

        // Handle null values properly
        String existingText = existing.getReviewText() != null ? existing.getReviewText() : "";
        String newText = newReview.getReviewText() != null ? newReview.getReviewText() : "";

        if (!existingText.equals(newText)) {
            existing.setReviewText(newReview.getReviewText());
            hasChanges = true;
        }

        if (existing.getLikesCount() != newReview.getLikesCount()) {
            existing.setLikesCount(newReview.getLikesCount());
            hasChanges = true;
        }

        String existingResponse = existing.getOwnerResponse() != null ? existing.getOwnerResponse() : "";
        String newResponse = newReview.getOwnerResponse() != null ? newReview.getOwnerResponse() : "";

        if (!existingResponse.equals(newResponse)) {
            existing.setOwnerResponse(newReview.getOwnerResponse());
            existing.setOwnerResponseDate(newReview.getOwnerResponseDate());
            hasChanges = true;
        }

        return hasChanges;
    }

    public List<Review> getActiveReviews(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "reviewDate"));
        return reviewRepository.findByIsActiveTrueOrderByReviewDateDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    public List<Review> getReviewsByRating(int minRating) {
        return reviewRepository.findByRatingGreaterThanEqualAndIsActiveTrueOrderByReviewDateDesc(minRating);
    }

    public long getTotalReviewsCount() {
        return reviewRepository.countByIsActiveTrue();
    }

    public double getAverageRating() {
        List<Review> reviews = reviewRepository.findByIsActiveTrueOrderByReviewDateDesc();
        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}