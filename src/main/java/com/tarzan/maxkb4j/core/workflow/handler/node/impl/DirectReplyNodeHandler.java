package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.tarzan.maxkb4j.common.util.ObjectUtil;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.INode;
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
            Object value = workflow.getReferenceField(fields.get(0), fields.get(1));
            if (value == null) {
                node.setAnswerText("None");
            } else if (ObjectUtil.isSimpleType(value)) {
                node.setAnswerText(value.toString());
            } else {
                node.setAnswerText(JSON.toJSONString(value));
            }
        } else {
            node.setAnswerText(workflow.generatePrompt(nodeParams.getContent()));
        }
        return new NodeResult(Map.of("answer", node.getAnswerText()), Map.of());
    }

}
