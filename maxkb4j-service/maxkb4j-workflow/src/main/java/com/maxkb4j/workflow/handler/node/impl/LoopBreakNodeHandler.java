package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopBreakNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_BREAK)
@Component
public class LoopBreakNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        LoopBreakNode.NodeParams nodeParams= node.getNodeData().toJavaObject(LoopBreakNode.NodeParams.class);
        boolean isBreak= ConditionUtil.assertion(workflow, nodeParams.getCondition(), nodeParams.getConditionList());
        node.getDetail().put("is_break",isBreak);
        if (isBreak){
            node.setAnswerText("BREAK");
        }
        return new NodeResult(Map.of());
    }

}
