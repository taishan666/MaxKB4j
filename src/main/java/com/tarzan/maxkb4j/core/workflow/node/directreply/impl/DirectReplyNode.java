package com.tarzan.maxkb4j.core.workflow.node.directreply.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.directreply.input.ReplyNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.REPLY;

public class DirectReplyNode extends INode {

    public DirectReplyNode(JSONObject properties) {
        super(properties);
        this.type = REPLY.getKey();
    }


    @Override
    public NodeResult execute() {
        ReplyNodeParams nodeParams = super.getNodeData().toJavaObject(ReplyNodeParams.class);
        String result;
        if ("referencing".equals(nodeParams.getReplyType())) {
            List<String> fields = nodeParams.getFields();
            Object res = super.getReferenceField(fields.get(0), fields.get(1));
            if (res != null){
                res = JSON.toJSONString(res);
            }
            result = res == null ? "" : res.toString();
        } else {
            result = super.generatePrompt(nodeParams.getContent());
        }
        return new NodeResult(Map.of("answer", result), Map.of());
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer", context.get("answer"));
        return detail;
    }

}
