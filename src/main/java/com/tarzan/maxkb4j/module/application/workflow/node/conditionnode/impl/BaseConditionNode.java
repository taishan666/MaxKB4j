package com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.impl;

import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.IConditionNode;
import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.dto.Condition;
import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.dto.ConditionBranch;
import com.tarzan.maxkb4j.module.application.workflow.node.conditionnode.dto.ConditionNodeParams;

import java.util.List;
import java.util.Map;

public class BaseConditionNode extends IConditionNode {
    @Override
    public NodeResult execute(ConditionNodeParams nodeParams) {
        ConditionBranch branch = _execute(nodeParams.getBranch());
        assert branch != null;
        return new NodeResult(Map.of("branch_id", branch.getId(), "branch_name", branch.getType()), Map.of());
    }
    private ConditionBranch _execute(List<ConditionBranch> branchList) {
        for (ConditionBranch branch : branchList) {
            if (branchAssertion(branch)) {
                return branch;
            }
        }
        return null; // In case no branch matches the assertion.
    }

    // Method to assert branches based on conditions.
    private boolean branchAssertion(ConditionBranch branch) {
        List<Condition> conditions = branch.getConditions();
        String conditionType = branch.getCondition();

        boolean result = conditionType.equals("and");
        for (Condition row : conditions) {
            boolean conditionResult = assertion(row.getField(),
                    row.getCompare(),
                    row.getValue());
            if (conditionType.equals("and")) {
                result &= conditionResult;
            } else {
                result |= conditionResult;
            }
        }
        return result;
    }

    // Method to perform assertions on fields.
    private boolean assertion(List<String> fieldList, String compareOperation, Object valueToCompare) {
        // Implementation of getting field_value would depend on how you implement workflow_manage in Java.
        // For simplicity, assume we have a method called getField which works similarly.
        Object fieldValue = super.workflowManage.getReferenceField(fieldList.get(0),fieldList.subList(1, fieldList.size()));

        // Implementation of compare handlers should be done as separate classes implementing an interface or abstract class.
        // Here we just simulate it with a placeholder.
        if (!compareOperation.equals("equals")) return false;
        assert fieldValue != null;
        return fieldValue.equals(valueToCompare);
    }

    @Override
    public void saveContext(NodeDetail nodeDetail, WorkflowManage workflowManage) {
        super.context.put("branch_id",nodeDetail.getBranchId());
        super.context.put("'branch_name'",nodeDetail.getBranchName());
    }
}
