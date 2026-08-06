package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.LOOP_BREAK)
public class LoopBreakNode extends AbsNode {
    public LoopBreakNode(String id, JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("is_break", detail.get("is_break"));
    }

    @Data
    public static class NodeParams {
        private String condition;
        private List<Condition> conditionList;
    }

}
