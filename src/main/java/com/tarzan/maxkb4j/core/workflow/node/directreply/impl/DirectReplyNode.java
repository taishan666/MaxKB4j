package com.tarzan.maxkb4j.core.workflow.node.directreply.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.REPLY;

public class DirectReplyNode extends INode {

    public DirectReplyNode(JSONObject properties) {
        super(properties);
        super.setType(REPLY.getKey());
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



}
