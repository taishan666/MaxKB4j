package com.tarzan.maxkb4j.core.workflow.node.function.input;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FunctionParams{
    private List<Map<String,Object>> inputFieldList;
    private String code;
    private Boolean isResult;
}
