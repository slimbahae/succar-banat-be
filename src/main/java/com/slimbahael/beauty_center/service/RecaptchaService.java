package com.slimbahael.beauty_center.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecaptchaService {

    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret.key}")
    private String recaptchaSecretKey;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verify(String recaptchaToken) {
        if (recaptchaToken == null || recaptchaToken.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", recaptchaSecretKey);
        params.add("response", recaptchaToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(VERIFY_URL, request, String.class);
            String body = response.getBody();
            return body != null && body.contains("\"success\": true");
        } catch (Exception e) {
            log.error("Error verifying reCAPTCHA: {}", e.getMessage());
            return false;
        }
    }
}
