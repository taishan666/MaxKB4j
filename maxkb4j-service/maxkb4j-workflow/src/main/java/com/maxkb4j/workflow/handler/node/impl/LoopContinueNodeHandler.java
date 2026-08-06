package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopContinueNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_CONTINUE)
@Component
public class LoopContinueNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        LoopContinueNode.NodeParams params = parseParams(node, LoopContinueNode.NodeParams.class);
        boolean isContinue = ConditionUtil.assertion(workflow, params.getCondition(), params.getConditionList());

        if (isContinue) {
            return new NodeResult(Map.of("is_continue", true, "branchId", "continue"));
        }
        return new NodeResult(Map.of("is_continue", false));
    }
}
