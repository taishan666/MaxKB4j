package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@NodeHandlerType(NodeType.LOOP_START_NODE)
@Component
public class LoopStartNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        Map<String, Object> nodeVariable = new HashMap<>();
        
        Map<String, Object> loopParams = (Map<String, Object>) workflow.getContext().getOrDefault("loop_params", new HashMap<>());
        
        nodeVariable.put("index", loopParams.getOrDefault("index", 0));
        nodeVariable.put("item", workflow.getContext().get("item"));
        nodeVariable.put("loop_start", true);
        String answerText = workflow.getContext().get("item").toString();
        node.setAnswerText(answerText);
        nodeVariable.put("is_submit", false);
        return new NodeResult(Map.of("answer", node.getAnswerText()));
    }
}
