package com.maxkb4j.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SourceResource {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private String type;
    private String UserId;
}
