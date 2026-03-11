package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.LOOP;


public class LoopNode extends AbsNode {
    public LoopNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(LOOP.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("current_index", detail.get("current_index"));
    }


    @Data
    public static class NodeParams {
        private String loopType;
        private JSONObject loopBody;
        private Integer number;
        private List<String> array;
    }
}
