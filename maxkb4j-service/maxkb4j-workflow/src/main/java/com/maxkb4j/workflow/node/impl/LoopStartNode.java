package com.maxkb4j.workflow.node.impl;
import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.LOOP_START)
public class LoopStartNode extends AbsNode {
    public LoopStartNode(String id, JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("index", detail.get("current_index"));
        context.put("item", detail.get("current_item"));
        JSONArray loopInputFieldList= (JSONArray) detail.get("loopInputFieldList");
        for (int i = 0; i < loopInputFieldList.size(); i++) {
            JSONObject loopInputField=loopInputFieldList.getJSONObject(i);
            String key=loopInputField.getString("field");
            Object value=loopInputField.get("value");
            workflow.getLoopContext().put(key, value);
        }
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
