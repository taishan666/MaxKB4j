package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.VARIABLE_SPLITTING)
public class VariableSplittingNode extends AbsNode {
    public VariableSplittingNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        Map<String, Object> result = (Map<String, Object>) detail.get(NodeField.RESULT);
        if (result != null){
            context.putAll(result);
        }
        context.put(NodeField.RESULT, result);
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
