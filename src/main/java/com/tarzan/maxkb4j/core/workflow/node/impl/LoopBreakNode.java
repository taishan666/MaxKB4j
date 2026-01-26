package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.LOOP_BREAK_NODE;

public class LoopBreakNode extends AbsNode {
    public LoopBreakNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(LOOP_BREAK_NODE.getKey());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("exception_message", detail.get("err_message"));
        context.put("is_break", detail.get("is_break"));
    }


    @Data
    public static class NodeParams {
        private String condition;
        private List<Condition> conditionList;
        
        @Data
        public static class Condition {
            private List<String> field;
            private String compare;
            private String value;
        }
    }
}
