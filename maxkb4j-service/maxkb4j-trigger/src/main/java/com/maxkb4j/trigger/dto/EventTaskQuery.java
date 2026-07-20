package com.maxkb4j.trigger.dto;

import lombok.Data;

@Data
public class EventTaskQuery {
    private String name;
    private String state;
    private String sourceType;
    private String order;
}
