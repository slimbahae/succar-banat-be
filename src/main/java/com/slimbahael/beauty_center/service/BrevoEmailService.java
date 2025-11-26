package com.slimbahael.beauty_center.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${app.business.email:noreply@beautycenter.com}")
    private String fromEmail;

    @Value("${app.business.name:Beauty Center}")
    private String fromName;

    public void sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("sender", Map.of("name", fromName, "email", fromEmail));
            emailData.put("to", List.of(Map.of("email", toEmail, "name", toName)));
            emailData.put("subject", subject);
            emailData.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent successfully via Brevo API to: {}", toEmail);
            } else {
                log.error("Failed to send email via Brevo API. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send email via Brevo API");
            }

        } catch (Exception e) {
            log.error("Error sending email via Brevo API to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email via Brevo API", e);
        }
    }
}
