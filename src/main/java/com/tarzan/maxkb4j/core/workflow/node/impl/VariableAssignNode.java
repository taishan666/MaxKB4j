package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_ASSIGN;

public class VariableAssignNode extends AbsNode {
    public VariableAssignNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(VARIABLE_ASSIGN.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("variableList", detail.get("variableList"));
        context.put("resultList", detail.get("resultList"));
    }

    @Data
    public static class NodeParams{
        private List<Map<String, Object>> variableList;
    }



}
