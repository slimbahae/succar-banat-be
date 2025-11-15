package com.slimbahael.beauty_center.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    public void sendSms(String phoneNumber, String message) {
        // For now, we'll just log the SMS since we're implementing a mock
        log.info("Sending SMS to {}: {}", phoneNumber, message);

        // In a real implementation, you would integrate with an SMS service
        // like Twilio, Vonage (formerly Nexmo), or any other SMS provider
    }
}