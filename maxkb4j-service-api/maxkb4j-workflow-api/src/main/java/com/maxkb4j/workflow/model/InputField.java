package com.maxkb4j.workflow.model;

import lombok.Data;

import java.util.List;

@Data
public class InputField {
    private String field;
    private List<String> value;
    private Object defaultValue;
}
