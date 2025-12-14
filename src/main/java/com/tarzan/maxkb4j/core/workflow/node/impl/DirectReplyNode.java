package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.REPLY;

public class DirectReplyNode extends INode {

    public DirectReplyNode(JSONObject properties) {
        super(properties);
        super.setType(REPLY.getKey());
    }

    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("answer", detail.get("answer"));
    }


    @Data
    public static class NodeParams {
        private String replyType;
        private List<String> fields;
        private String content;
        private Boolean isResult;
    }

}
