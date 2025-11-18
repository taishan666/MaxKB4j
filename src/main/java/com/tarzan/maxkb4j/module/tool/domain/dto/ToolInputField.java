package com.tarzan.maxkb4j.module.tool.domain.dto;

import lombok.Data;

@Data
public class ToolInputField {

    private String name;
    private String type;
    private String source;
    private Boolean isRequired;
    private Object value;
}
