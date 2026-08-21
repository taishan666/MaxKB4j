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
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.LOOP_START)
public class LoopStartNode extends AbsNode {
    public LoopStartNode(String id, JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(RuntimeDetailField.INDEX, detail.get(LoopField.CURRENT_INDEX));
        context.put(LoopField.ITEM, detail.get(LoopField.CURRENT_ITEM));
        JSONArray loopInputFieldList= (JSONArray) detail.get(LoopField.LOOP_INPUT_FIELD_LIST);
        for (int i = 0; i < loopInputFieldList.size(); i++) {
            JSONObject loopInputField=loopInputFieldList.getJSONObject(i);
            String key=loopInputField.getString("field");
            Object value=loopInputField.get(VariableField.VALUE);
            workflow.getLoopContext().put(key, value);
        }
    }

    @Override
    public Map<String, Object> getDetail() {
        detail.put(LoopField.CURRENT_INDEX, context.get(RuntimeDetailField.INDEX));
        detail.put(LoopField.CURRENT_ITEM, context.get(LoopField.ITEM));
        detail.remove(RuntimeDetailField.INDEX);
        detail.remove(LoopField.ITEM);
        return detail;
    }

    @Data
    public static class NodeParams {
        private List<JSONObject> loopInputFieldList;
    }
}
