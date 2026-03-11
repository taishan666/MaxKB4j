package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ConditionNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.CONDITION)
@Component
public class ConditionNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        ConditionNode.NodeParams nodeParams= node.getNodeData().toJavaObject(ConditionNode.NodeParams.class);
        ConditionNode.Branch branch = _execute(workflow,nodeParams.getBranch());
        assert branch != null;
        return new NodeResult(Map.of("branchId", branch.getId(), "branchName", branch.getType()));
    }

    private ConditionNode.Branch _execute(Workflow workflow,List<ConditionNode.Branch> branchList) {
        for (ConditionNode.Branch branch : branchList) {
            if (ConditionUtil.assertion(workflow, branch.getCondition(),branch.getConditions())) {
                return branch;
            }
        }
        return null; // In case no branch matches the assertion.
    }

}
