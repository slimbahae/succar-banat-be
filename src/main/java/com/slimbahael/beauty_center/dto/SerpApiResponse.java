package com.slimbahael.beauty_center.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SerpApiResponse {

    @JsonProperty("reviews")
    private List<GoogleReviewDto> reviews;

    @JsonProperty("serpapi_pagination")
    private PaginationDto pagination;

    @Data
    public static class GoogleReviewDto {

        @JsonProperty("position")
        private int position;

        @JsonProperty("user")
        private UserDto user;

        @JsonProperty("rating")
        private int rating;

        @JsonProperty("date")
        private String date;

        @JsonProperty("snippet")
        private String snippet;

        @JsonProperty("likes")
        private Integer likes;

        @JsonProperty("images")
        private List<String> images;

        @JsonProperty("response")
        private ResponseDto response;

        @Data
        public static class UserDto {
            @JsonProperty("name")
            private String name;

            @JsonProperty("link")
            private String link;

            @JsonProperty("thumbnail")
            private String thumbnail;

            @JsonProperty("local_guide")
            private Boolean localGuide;

            @JsonProperty("reviews")
            private Integer reviews;

            @JsonProperty("photos")
            private Integer photos;
        }

        @Data
        public static class ResponseDto {
            @JsonProperty("date")
            private String date;

            @JsonProperty("snippet")
            private String snippet;
        }
    }

    @Data
    public static class PaginationDto {
        @JsonProperty("next_page_token")
        private String nextPageToken;

        @JsonProperty("current")
        private int current;

        @JsonProperty("other_pages")
        private Object otherPages;
    }
}
