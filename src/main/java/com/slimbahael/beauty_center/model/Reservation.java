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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reservations")
public class Reservation {

    @Id
    private String id;

    @Indexed
    private String customerId;

    @Indexed
    private String staffId;

    @Indexed
    private Date reservationDate;

    private String timeSlot; // "MORNING" or "EVENING"

    private String serviceId;

    private List<String> addonIds;

    private String status; // "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"

    private BigDecimal totalAmount;

    private Date createdAt;

    private Date updatedAt;

    // For notifications
    private boolean smsReminderSent;

    // Special requests or notes from the customer
    private String notes;
}