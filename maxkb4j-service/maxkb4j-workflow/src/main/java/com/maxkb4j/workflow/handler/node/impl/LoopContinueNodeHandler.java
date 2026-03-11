package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopContinueNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_CONTINUE)
@Component
public class LoopContinueNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        LoopContinueNode.NodeParams nodeParams= node.getNodeData().toJavaObject(LoopContinueNode.NodeParams.class);
        boolean isContinue = ConditionUtil.assertion(workflow, nodeParams.getCondition(), nodeParams.getConditionList());
        if (isContinue){
            return new NodeResult(Map.of("is_continue", true,"branchId", "continue"));
        }
        return new NodeResult(Map.of("is_continue", false));
    }

}
