package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NodeCreatorType(NodeType.LOOP)
public class LoopNode extends AbsNode {
    public LoopNode(String id, JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put("current_index", detail.get("current_index"));
    }

    @Override
    public List<Answer> getAnswerList(String chatRecordId) {
        //todo
        return  Collections.emptyList();
    }

    @Data
    public static class NodeParams {
        private String loopType;
        private JSONObject loopBody;
        private Integer number;
        private List<String> array;
    }
}
