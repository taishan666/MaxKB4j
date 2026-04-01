package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.model.params.ConditionNodeParams;
import com.maxkb4j.workflow.util.ConditionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.CONDITION)
@Component
@RequiredArgsConstructor
public class ConditionNodeHandler extends AbstractNodeHandler<ConditionNodeParams> {

    private final ConditionUtil conditionUtil;

    @Override
    protected Class<ConditionNodeParams> getParamsClass() {
        return ConditionNodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, ConditionNodeParams params) throws Exception {
        ConditionNodeParams.Branch branch = executeBranch(workflow, params.getBranch());
        assert branch != null;
        return new NodeResult(Map.of("branchId", branch.getId(), "branchName", branch.getType()));
    }

    private ConditionNodeParams.Branch executeBranch(Workflow workflow, List<ConditionNodeParams.Branch> branchList) {
        for (ConditionNodeParams.Branch branch : branchList) {
            if (conditionUtil.assertion(workflow, branch.getCondition(), branch.getConditions())) {
                return branch;
            }
        }
        return null;
    }

}
