package com.tarzan.maxkb4j.core.workflow.model;

import lombok.Data;

import java.util.List;

@Data
public class NodeField {
    private String field;
    private List<String> value;
}
