package com.maxkb4j.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomeQuery {
    @NotBlank
    private String startTime;
    @NotBlank
    private String endTime;
    private String name;
    private String applicationId;
}
