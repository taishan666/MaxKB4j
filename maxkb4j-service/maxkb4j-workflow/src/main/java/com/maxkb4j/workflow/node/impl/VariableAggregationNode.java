package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.VARIABLE_AGGREGATE;


public class VariableAggregationNode extends AbsNode {
    public VariableAggregationNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(VARIABLE_AGGREGATE.getKey());
    }


    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        List<Group> groupList= (List<Group>) detail.get("groupList");
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
