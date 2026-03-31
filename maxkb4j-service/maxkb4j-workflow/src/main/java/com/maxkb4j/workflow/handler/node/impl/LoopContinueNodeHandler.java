package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopContinueNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_CONTINUE)
@Component
public class LoopContinueNodeHandler extends AbstractNodeHandler<LoopContinueNode.NodeParams> {

    @Override
    protected Class<LoopContinueNode.NodeParams> getParamsClass() {
        return LoopContinueNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, LoopContinueNode.NodeParams params) throws Exception {
        boolean isContinue = ConditionUtil.assertion(workflow, params.getCondition(), params.getConditionList());

        if (isContinue) {
            return buildResult(Map.of("is_continue", true, "branchId", "continue"));
        }
        return buildResult(Map.of("is_continue", false));
    }
}
