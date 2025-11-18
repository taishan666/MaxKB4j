package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_AGGREGATE;


public class VariableAggregationNode extends INode {
    public VariableAggregationNode(JSONObject properties) {
        super(properties);
        this.setType(VARIABLE_AGGREGATE.getKey());
    }


    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        List<VariableAggregationNode.Group> groupList= (List<Group>) detail.get("groupList");
        for (Group group : groupList) {
            context.put(group.getField(), group.getValue());
        }
    }

    @Data
    public static class NodeParams{
        private String strategy;
        private List<Group> groupList;
    }

    @Data
    public static class Group{
        private String id;
        private String label;
        private String field;
        private Object value;
        private List<Variable> variableList;
    }

    @Data
    public static class Variable{
        private String nodeName;
        private String field;
        private Object value;
        private List<String> variable;
    }
}
