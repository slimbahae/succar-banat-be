package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Data
public class AvailabilityRequest {

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    @NotNull(message = "Date is required")
    private Date date;
}