package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson2.JSON;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.impl.DirectReplyNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DirectReplyNodeHandler implements INodeHandler {
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        DirectReplyNode.NodeParams nodeParams = node.getNodeData().toJavaObject(DirectReplyNode.NodeParams.class);
        if ("referencing".equals(nodeParams.getReplyType())) {
            List<String> fields = nodeParams.getFields();
            Object res = workflow.getReferenceField(fields.get(0), fields.get(1));
            if (res == null) {
                node.setAnswerText("");
            } else if (isSimpleType(res)) {
                node.setAnswerText(res.toString());
            } else {
                node.setAnswerText(JSON.toJSONString(res));
            }
        } else {
            node.setAnswerText(workflow.generatePrompt(nodeParams.getContent()));
        }
        return new NodeResult(Map.of("answer", node.getAnswerText()), Map.of());
    }

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
}
