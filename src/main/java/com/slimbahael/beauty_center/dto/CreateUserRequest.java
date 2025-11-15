package com.slimbahael.beauty_center.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password should be at least 6 characters")
    private String password;

    private String phoneNumber;

    @NotBlank(message = "Role is required")
    private String role;

    private String profileImage;

    // For staff members
    private List<String> specialties;
    private List<String> workDays;
    private String morningShift;
    private String eveningShift;
}