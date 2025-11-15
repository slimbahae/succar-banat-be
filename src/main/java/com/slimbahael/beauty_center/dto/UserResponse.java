package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String role;
    private String profileImage;
    private List<String> specialties;
    private List<String> workDays;
    private String morningShift;
    private String eveningShift;
    private boolean enabled;
}