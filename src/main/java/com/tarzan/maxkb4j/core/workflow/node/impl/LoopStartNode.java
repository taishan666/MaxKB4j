package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.LOOP_START;

public class LoopStartNode extends AbsNode {
    public LoopStartNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(LOOP_START.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("index", detail.get("current_index"));
        context.put("item", detail.get("current_item"));
    }

    @Override
    public Map<String, Object> getDetail() {
        detail.put("current_index", context.get("index"));
        detail.put("current_item", context.get("item"));
        detail.remove("index");
        detail.remove("item");
        return detail;
    }


    @Data
    public static class NodeParams {
        private List<JSONObject> loopInputFieldList;
    }
}
