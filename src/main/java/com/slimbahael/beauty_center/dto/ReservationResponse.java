package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private String id;
    private String customerId;
    private String customerName;
    private String staffId;
    private String staffName;
    private Date reservationDate;
    private String timeSlot;
    private String serviceId;
    private String serviceName;
    private List<ServiceAddonInfo> addons;
    private String status;
    private BigDecimal totalAmount;
    private Date createdAt;
    private String notes;
    private boolean smsReminderSent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceAddonInfo {
        private String id;
        private String name;
        private BigDecimal price;
    }
}