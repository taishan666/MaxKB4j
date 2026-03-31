package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ConditionNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.CONDITION)
@Component
@RequiredArgsConstructor
public class ConditionNodeHandler extends AbstractNodeHandler<ConditionNode.NodeParams> {

    private final ConditionUtil conditionUtil;

    @Override
    protected Class<ConditionNode.NodeParams> getParamsClass() {
        return ConditionNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, ConditionNode.NodeParams params) throws Exception {
        ConditionNode.Branch branch = executeBranch(workflow, params.getBranch());
        assert branch != null;
        return buildResult(Map.of("branchId", branch.getId(), "branchName", branch.getType()));
    }

    private ConditionNode.Branch executeBranch(Workflow workflow, List<ConditionNode.Branch> branchList) {
        for (ConditionNode.Branch branch : branchList) {
            if (conditionUtil.assertion(workflow, branch.getCondition(), branch.getConditions())) {
                return branch;
            }
        }
        return null; // In case no branch matches the assertion.
    }

}
