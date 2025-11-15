package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private Date date;
    private boolean morningAvailable;
    private boolean eveningAvailable;
    private List<StaffAvailability> availableStaff;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffAvailability {
        private String staffId;
        private String staffName;
        private String staffImage;
        private boolean availableMorning;
        private boolean availableEvening;
    }
}