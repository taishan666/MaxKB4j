package com.tarzan.maxkb4j.core.workflow.node.variableaggregate.input;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VariableAggregateParams {

    private List<Map<String, Object>> variableList;
}
