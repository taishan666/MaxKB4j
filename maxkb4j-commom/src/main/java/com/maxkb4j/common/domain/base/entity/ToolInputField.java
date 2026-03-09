package com.maxkb4j.common.domain.base.entity;

import lombok.Data;

@Data
public class ToolInputField {

    private String name;
    private String type;
    private String source;
    private Boolean isRequired;
    private Object value;
}
