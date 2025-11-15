package com.slimbahael.beauty_center.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAddonResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer additionalDuration;
    private List<String> compatibleServiceIds;
    private boolean active;
}