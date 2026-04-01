package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopContinueNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_CONTINUE)
@Component
@RequiredArgsConstructor
public class LoopContinueNodeHandler extends AbstractNodeHandler {

    private final ConditionUtil conditionUtil;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        LoopContinueNode.NodeParams params = parseParams(node, LoopContinueNode.NodeParams.class);
        boolean isContinue = conditionUtil.assertion(workflow, params.getCondition(), params.getConditionList());

        if (isContinue) {
            return new NodeResult(Map.of("is_continue", true, "branchId", "continue"));
        }
        return new NodeResult(Map.of("is_continue", false));
    }
}
