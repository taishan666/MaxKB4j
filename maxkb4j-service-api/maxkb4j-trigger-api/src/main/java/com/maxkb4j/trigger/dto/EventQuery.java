package com.maxkb4j.trigger.dto;

import lombok.Data;

@Data
public class EventQuery {
    private String name;
    private String createUser;
    private String type;
    private Boolean isActive;
}
