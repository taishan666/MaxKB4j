package com.tarzan.maxkb4j.core.workflow.node.variableassign.input;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VariableAssignParams{

    private List<Map<String, Object>> variableList;
}
