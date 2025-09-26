package com.tarzan.maxkb4j.core.workflow.node.condition.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.condition.compare.Compare;
import com.tarzan.maxkb4j.core.workflow.node.condition.compare.impl.*;
import com.tarzan.maxkb4j.core.workflow.node.condition.input.Condition;
import com.tarzan.maxkb4j.core.workflow.node.condition.input.ConditionBranch;
import com.tarzan.maxkb4j.core.workflow.node.condition.input.ConditionNodeParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.CONDITION;

public class ConditionNode extends INode {

    static List<Compare> compareHandleList = new ArrayList<>();

    static {
        compareHandleList.add(new GECompare());
        compareHandleList.add(new GTCompare());
        compareHandleList.add(new ContainCompare());
        compareHandleList.add(new EqualCompare());
        compareHandleList.add(new LTCompare());
        compareHandleList.add(new LECompare());
        compareHandleList.add(new LengthLECompare());
        compareHandleList.add(new LengthLTCompare());
        compareHandleList.add(new LengthEqualCompare());
        compareHandleList.add(new LengthGECompare());
        compareHandleList.add(new LengthGTCompare());
        compareHandleList.add(new IsNullCompare());
        compareHandleList.add(new IsNotNullCompare());
        compareHandleList.add(new NotContainCompare());
    }

    public ConditionNode(JSONObject properties) {
        super(properties);
        this.type = CONDITION.getKey();
    }


    @Override
    public NodeResult execute() {
        ConditionNodeParams nodeParams= super.getNodeData().toJavaObject(ConditionNodeParams.class);
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
    private boolean assertion(List<String> fieldList, String compare, String valueToCompare) {
        Object fieldValue = super.getReferenceField(fieldList.get(0),fieldList.get(1));
        for (Compare compareHandler : compareHandleList) {
            if(compareHandler.support(compare)){
                return compareHandler.compare(fieldValue, valueToCompare);
            }
        }
        return false;
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("branch_id",context.get("branch_id"));
        detail.put("branch_name",context.get("branch_name"));
        return detail;
    }

}
