package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_SPLITTING;

public class VariableSplittingNode extends INode {
    public VariableSplittingNode(JSONObject properties) {
        super(properties);
        this.setType(VARIABLE_SPLITTING.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("variableList", detail.get("variableList"));
        context.put("resultList", detail.get("resultList"));
    }

    @Data
    public static class NodeParams{
        private List<String> inputVariable;
        private List<Variable> variableList;
    }

    @Data
    public static class Variable{
        private String field;
        private String label;
        private String expression;

    }



}
