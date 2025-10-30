package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.compare.Compare;
import com.tarzan.maxkb4j.core.workflow.compare.impl.*;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.impl.ConditionNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ConditionNodeHandler implements INodeHandler {

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

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ConditionNode.NodeParams nodeParams= node.getNodeData().toJavaObject(ConditionNode.NodeParams.class);
        ConditionNode.Branch branch = _execute(workflow,nodeParams.getBranch());
        assert branch != null;
        return new NodeResult(Map.of("branchId", branch.getId(), "branchName", branch.getType()), Map.of());
    }

    private ConditionNode.Branch _execute(Workflow workflow,List<ConditionNode.Branch> branchList) {
        for (ConditionNode.Branch branch : branchList) {
            if (branchAssertion(workflow,branch)) {
                return branch;
            }
        }
        return null; // In case no branch matches the assertion.
    }

    private boolean branchAssertion(Workflow workflow,ConditionNode.Branch branch) {
        List<ConditionNode.Condition> conditions = branch.getConditions();
        String conditionType = branch.getCondition();
        boolean result = conditionType.equals("and");
        for (ConditionNode.Condition row : conditions) {
            boolean conditionResult = assertion(workflow,row.getField(),
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

    private boolean assertion(Workflow workflow,List<String> fieldList, String compare, String valueToCompare) {
        Object fieldValue = workflow.getReferenceField(fieldList.get(0),fieldList.get(1));
        for (Compare compareHandler : compareHandleList) {
            if(compareHandler.support(compare)){
                return compareHandler.compare(fieldValue, valueToCompare);
            }
        }
        return false;
    }
}
