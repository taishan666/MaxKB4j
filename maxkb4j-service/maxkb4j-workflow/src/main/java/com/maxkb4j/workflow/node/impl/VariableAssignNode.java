package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.VARIABLE_ASSIGN;

@NodeCreatorType(NodeType.VARIABLE_ASSIGN)
public class VariableAssignNode extends AbsNode {
    public VariableAssignNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(VARIABLE_ASSIGN.getKey());
    }


    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("variableList", detail.get("variableList"));
        context.put("resultList", detail.get("resultList"));
    }

    @Data
    public static class NodeParams{
        private List<Map<String, Object>> variableList;
    }



}
