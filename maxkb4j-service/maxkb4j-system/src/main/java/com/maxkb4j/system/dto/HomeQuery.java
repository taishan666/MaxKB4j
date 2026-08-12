package com.maxkb4j.system.dto;

import lombok.Data;

@Data
public class HomeQuery {
    private String startTime;
    private String endTime;
    private String name;
    private String applicationId;
}
