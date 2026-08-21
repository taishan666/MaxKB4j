package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.LoopField;

@NodeCreatorType(NodeType.LOOP_BREAK)
public class LoopBreakNode extends AbsNode {
    public LoopBreakNode(String id, JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(LoopField.IS_BREAK, detail.get(LoopField.IS_BREAK));
    }


    @Data
    public static class NodeParams {
        private String condition;
        private List<Condition> conditionList;
    }

}
