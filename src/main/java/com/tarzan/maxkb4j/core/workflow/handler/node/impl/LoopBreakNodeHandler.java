package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.common.util.ConditionUtil;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.LoopBreakNode;
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
