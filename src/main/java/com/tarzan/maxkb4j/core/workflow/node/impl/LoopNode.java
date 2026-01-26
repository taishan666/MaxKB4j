package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.LOOP_NODE;

public class LoopNode extends AbsNode {
    public LoopNode(String id, JSONObject properties) {
        super(id,properties);
        super.setType(LOOP_NODE.getKey());
        super.setViewType("single_view");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("loopType", detail.get("loopType"));
        context.put("number", detail.get("number"));
        context.put("is_result", detail.get("is_result"));
        context.put("result", detail.get("result"));
        context.put("loopBody", detail.get("loopBody"));
        context.put("run_time", detail.get("run_time"));

        
        Map<String, Object> array = (Map<String, Object>) detail.get("array");
        if (array != null) {
            context.putAll(array);
        }
        
        setAnswerText("");

    }


    @Data
    public static class NodeParams {
        private String loopType;
        private JSONObject loopBody;
        private Integer number;
        private List<String> array;
    }
}
