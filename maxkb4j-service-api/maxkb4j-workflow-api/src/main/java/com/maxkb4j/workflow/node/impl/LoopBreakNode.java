package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.LOOP_BREAK;


public class LoopBreakNode extends AbsNode {
    public LoopBreakNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(LOOP_BREAK.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("is_break", detail.get("is_break"));
    }


    @Data
    public static class NodeParams {
        private String condition;
        private List<Condition> conditionList;
    }

}
