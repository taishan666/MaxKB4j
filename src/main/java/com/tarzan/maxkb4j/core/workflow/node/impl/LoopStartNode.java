package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.LOOP_START_NODE;

public class LoopStartNode extends AbsNode {
    public LoopStartNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(LOOP_START_NODE.getKey());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("index", detail.get("current_index"));
        context.put("item", detail.get("current_item"));
        context.put("exception_message", detail.get("err_message"));
    }


    @Data
    public static class NodeParams {
        private List<JSONObject> globalFields;
    }
}
