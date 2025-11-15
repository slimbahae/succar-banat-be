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
@Document(collection = "serviceAddons")
public class ServiceAddon {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    private BigDecimal price;

    // Additional duration in minutes (if any)
    private Integer additionalDuration;

    // The parent service IDs this addon can be applied to
    private List<String> compatibleServiceIds;

    private boolean active;
}