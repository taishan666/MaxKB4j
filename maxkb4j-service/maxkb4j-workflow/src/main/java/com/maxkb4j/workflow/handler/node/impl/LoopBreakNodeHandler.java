package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
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
public class LoopBreakNodeHandler extends AbstractNodeHandler<LoopBreakNode.NodeParams> {

    private final ConditionUtil conditionUtil;

    @Override
    protected Class<LoopBreakNode.NodeParams> getParamsClass() {
        return LoopBreakNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, LoopBreakNode.NodeParams params) throws Exception {
        boolean isBreak = conditionUtil.assertion(workflow, params.getCondition(), params.getConditionList());
        putDetail(node, "is_break", isBreak);

        if (isBreak) {
            setAnswer(node, "BREAK");
        }

        return buildResult(Map.of());
    }
}
