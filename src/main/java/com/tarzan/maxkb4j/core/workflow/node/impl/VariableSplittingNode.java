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
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        Map<String, Object> result = (Map<String, Object>) detail.get("result");
        if (result != null){
            context.putAll(result);
        }
        context.put("result", result);
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
