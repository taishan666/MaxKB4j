package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.common.util.ObjectUtil;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.DirectReplyNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@NodeHandlerType(NodeType.REPLY)
@Component
public class DirectReplyNodeHandler extends AbstractNodeHandler {


    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        DirectReplyNode.NodeParams params = parseParams(node, DirectReplyNode.NodeParams.class);
        AtomicReference<String> answerText = new AtomicReference<>("");
        if ("referencing".equals(params.getReplyType())) {
            List<String> fields = params.getFields();
            Object value = workflow.getReferenceField(fields);
            if (value == null) {
                answerText.set("None");
            } else if (ObjectUtil.isSimpleType(value)) {
                answerText.set(value.toString());
            } else {
                answerText.set(JSON.toJSONString(value));
            }
        } else {
            answerText.set(workflow.renderPrompt(params.getContent()));
        }
        if (params.getIsResult()) {
            setAnswer(node, answerText.get());
        }
        return new NodeResult(Map.of("answer", node.getAnswerText()));
    }
}
