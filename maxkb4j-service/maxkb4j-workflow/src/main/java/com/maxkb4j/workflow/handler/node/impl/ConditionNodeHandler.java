package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.ConditionNodeParams;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.ConditionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.CONDITION)
@Component
@RequiredArgsConstructor
public class ConditionNodeHandler extends AbsNodeHandler {

    private final ConditionUtil conditionUtil;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        ConditionNodeParams params= parseParams(node, ConditionNodeParams.class);
        ConditionNodeParams.Branch branch = executeBranch(workflow, params.getBranch());
        if (branch == null) {
            throw new ApiException("workflow.condition.no.match");
        }
        return new NodeResult(Map.of("branchId", branch.getId(), "branchName", branch.getType()));
    }

    private ConditionNodeParams.Branch executeBranch(IWorkflow workflow, List<ConditionNodeParams.Branch> branchList) {
        for (ConditionNodeParams.Branch branch : branchList) {
            if (conditionUtil.assertion(workflow, branch.getCondition(), branch.getConditions())) {
                return branch;
            }
        }
        return null;
    }

}
