package com.maxkb4j.workflow.node.impl;

import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.ContextValueUtil;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;

@NodeCreatorType(NodeType.VARIABLE_AGGREGATE)
public class VariableAggregationNode extends AbsNode {
    public VariableAggregationNode(String id, JSONObject properties) {
        super(id, properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        // 恢复执行的 detail 来自持久化 JSON，groupList 为 JSONArray（元素 JSONObject），
        // 需重建为 Group，否则迭代时抛 ClassCastException
        List<Group> groupList = ContextValueUtil.toList(detail.get(NodeField.GROUP_LIST), Group.class);
        for (Group group : groupList) {
            context.put(group.getField(), group.getValue());
        }
    }

    @Data
    public static class NodeParams {
        private String strategy;
        private List<Group> groupList;
    }

    @Data
    public static class Group {
        private String id;
        private String label;
        private String field;
        private Object value;
        private List<Variable> variableList;
    }

    @Data
    public static class Variable {
        private String nodeName;
        private String field;
        private Object value;
        private List<String> variable;
    }
}
