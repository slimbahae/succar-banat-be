package com.slimbahael.beauty_center.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private String userId;
    private String userName;
    private BigDecimal currentBalance;
    private BigDecimal pendingBalance;
    private String formattedBalance;
    private Date lastUpdated;
    private String status;
}