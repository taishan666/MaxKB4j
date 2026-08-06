package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopBreakNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_BREAK)
@Component
public class LoopBreakNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        LoopBreakNode.NodeParams params = parseParams(node, LoopBreakNode.NodeParams.class);
        boolean isBreak = ConditionUtil.assertion(workflow, params.getCondition(), params.getConditionList());
        putDetail(node, "is_break", isBreak);
        if (isBreak) {
            setAnswerText(node, "BREAK");
        }
        return new NodeResult(Map.of());
    }
}
