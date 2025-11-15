package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {

    private String clientSecret;
    private String paymentIntentId;
    private String status;
    private Long amount;
    private String currency;
}