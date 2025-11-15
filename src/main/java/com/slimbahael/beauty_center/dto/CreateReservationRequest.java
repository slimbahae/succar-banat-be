package com.slimbahael.beauty_center.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateReservationRequest {

    // Customer ID will be taken from authenticated user

    @NotBlank(message = "Staff ID is required")
    private String staffId;

    @NotNull(message = "Reservation date is required")
    private Date reservationDate;

    @NotBlank(message = "Time slot is required (MORNING or EVENING)")
    private String timeSlot;

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    private List<String> addonIds;

    private String notes;
}