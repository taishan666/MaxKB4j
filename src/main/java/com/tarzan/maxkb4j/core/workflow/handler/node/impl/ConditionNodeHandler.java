package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.common.util.ConditionUtil;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ConditionNode;
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
