package com.slimbahael.beauty_center.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

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
@Document(collection = "balance_transactions")
public class BalanceTransaction {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String transactionType; // CREDIT, DEBIT, REFUND, GIFT_CARD_REDEEM, GIFT_CARD_PURCHASE

    private BigDecimal amount;

    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    private String description;

    @Indexed
    private String status; // PENDING, COMPLETED, FAILED, CANCELLED

    // Reference to related entities
    private String orderId;
    private String reservationId;
    private String giftCardId;
    private String paymentIntentId;

    // Admin who performed the transaction (for manual adjustments)
    private String adminId;

    @Indexed
    private Date createdAt;
    private Date completedAt;

    private String notes;
}