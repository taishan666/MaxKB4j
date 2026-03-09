package com.maxkb4j.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AppTemplate {

    private String name;
    private String description;
    private String icon;
    private String type;
    private String downloadUrl;
    private String readMe;
    private String label;
}
