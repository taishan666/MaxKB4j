package com.tarzan.maxkb4j.core.workflow.node.directreply.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
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
        if ("referencing".equals(nodeParams.getReplyType())) {
            List<String> fields = nodeParams.getFields();
            Object res = super.getReferenceField(fields.get(0), fields.get(1));
            if (res == null) {
                answerText = "";
            } else if (isSimpleType(res)) {
                answerText = res.toString();
            } else {
                answerText = JSON.toJSONString(res);
            }
        } else {
            answerText = super.generatePrompt(nodeParams.getContent());
        }
        return new NodeResult(Map.of("answer", answerText), Map.of());
    }

    // 判断是否为简单类型（基本类型、包装类、String、Enum 等）
    private boolean isSimpleType(Object obj) {
        if (obj == null) return false;
        Class<?> clazz = obj.getClass();

        // 基本类型或其包装类 + String + Enum
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Boolean.class ||
                clazz == Character.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz.isEnum();
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }


    @Override
    public JSONObject getRunDetail() {
        return detail;
    }

}
