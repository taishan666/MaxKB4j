package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.LoopBreakNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.LOOP_BREAK_NODE)
@Component
public class LoopBreakNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        Map<String, Object> nodeVariable = new HashMap<>();
        LoopBreakNode.NodeParams nodeParams = node.getNodeData().toJavaObject(LoopBreakNode.NodeParams.class);
        
        String condition = nodeParams.getCondition();
        List<LoopBreakNode.NodeParams.Condition> conditionList = nodeParams.getConditionList();
        
        List<Boolean> results = conditionList.stream()
                .map(cond -> assertion(workflow, cond.getField(), cond.getCompare(), cond.getValue()))
                .toList();
        
        boolean isBreak = "and".equals(condition) ? results.stream().allMatch(b -> b) : results.stream().anyMatch(b -> b);
        
        node.getContext().put("is_break", isBreak);
        
        nodeVariable.put("is_break", isBreak);
        nodeVariable.put("loop_break", true);
        
        if (isBreak) {
            nodeVariable.put("content", "BREAK");
            nodeVariable.put("node_type", "loop-break-node");
        }
        
        return new NodeResult(nodeVariable, false, (n) -> isBreak);
    }

    private boolean assertion(Workflow workflow, List<String> fieldList, String compare, String value) {
        if (fieldList == null || fieldList.isEmpty()) {
            return false;
        }
        
        Object fieldValue = null;
        try {
            if (fieldList.size() > 1) {
                fieldValue = workflow.getReferenceField(fieldList.get(0), fieldList.get(1));
            } else {
                fieldValue = workflow.getReferenceField(fieldList.get(0), "");
            }
        } catch (Exception e) {
        }
        
        return compareValues(fieldValue, compare, value);
    }

    private boolean compareValues(Object fieldValue, String compare, String value) {
        if (fieldValue == null) {
            return false;
        }
        
        try {
            String fieldStr = fieldValue.toString();
            
            switch (compare) {
                case "eq":
                    return fieldStr.equals(value);
                case "neq":
                    return !fieldStr.equals(value);
                case "gt":
                    return Double.parseDouble(fieldStr) > Double.parseDouble(value);
                case "lt":
                    return Double.parseDouble(fieldStr) < Double.parseDouble(value);
                case "gte":
                    return Double.parseDouble(fieldStr) >= Double.parseDouble(value);
                case "lte":
                    return Double.parseDouble(fieldStr) <= Double.parseDouble(value);
                case "contains":
                    return fieldStr.contains(value);
                case "not_contains":
                    return !fieldStr.contains(value);
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
