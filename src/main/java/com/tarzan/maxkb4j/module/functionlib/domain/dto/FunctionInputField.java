package com.tarzan.maxkb4j.module.functionlib.domain.dto;

import lombok.Data;

@Data
public class FunctionInputField {

    private String name;
    private String type;
    private String source;
    private Boolean isRequired;
}
