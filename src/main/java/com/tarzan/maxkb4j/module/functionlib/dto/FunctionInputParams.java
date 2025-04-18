package com.tarzan.maxkb4j.module.functionlib.dto;

import lombok.Data;

@Data
public class FunctionInputParams {

    private String name;
    private String type;
    private String source;
    private Boolean isRequired;
}
