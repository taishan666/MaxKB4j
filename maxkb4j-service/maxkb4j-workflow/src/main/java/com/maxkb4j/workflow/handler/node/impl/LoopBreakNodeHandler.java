package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopBreakNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_BREAK)
@Component
@RequiredArgsConstructor
public class LoopBreakNodeHandler extends AbsNodeHandler {

    private final ConditionUtil conditionUtil;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        LoopBreakNode.NodeParams params = parseParams(node, LoopBreakNode.NodeParams.class);
        boolean isBreak = conditionUtil.assertion(workflow, params.getCondition(), params.getConditionList());
        putDetail(node, "is_break", isBreak);
        if (isBreak) {
            setAnswer(node, "BREAK");
        }
        return new NodeResult(Map.of());
    }
}
