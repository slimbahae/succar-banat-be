package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private String role;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;

    public JwtAuthResponse(String accessToken, String role, String userId, String firstName, String lastName, String email) {
        this.accessToken = accessToken;
        this.role = role;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}