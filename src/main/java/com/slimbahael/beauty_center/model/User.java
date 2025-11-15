package com.slimbahael.beauty_center.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String phoneNumber;

    @Indexed
    private String role; // ADMIN, STAFF, CUSTOMER

    private String profileImage;
    private boolean enabled;
    private List<String> specialties; // For STAFF only - services they can perform

    // For STAFF availability
    private List<String> workDays;
    private String morningShift; // "YES", "NO"
    private String eveningShift; // "YES", "NO"

    // NEW: Balance system fields
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO; // For pending transactions

    private java.util.Date lastBalanceUpdate;


}