package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.VariableAggregationNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class VariableAggregationNodeHandler implements INodeHandler {
    static Map<String, StrategyFunction> strategy_map = new HashMap<>();

    static {
        strategy_map.put("first_non_null", VariableAggregationNodeHandler::getFirstNonNull);
        strategy_map.put("variable_to_json", VariableAggregationNodeHandler::getCollection);
    }

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        VariableAggregationNode.NodeParams nodeParams = node.getNodeData().toJavaObject(VariableAggregationNode.NodeParams.class);
        String strategyName = nodeParams.getStrategy();
        Map<String, Object> nodeVariable = new HashMap<>();
        List<VariableAggregationNode.Group> groupList = nodeParams.getGroupList();
        for (VariableAggregationNode.Group group : groupList) {
            List<VariableAggregationNode.Variable> variableList = group.getVariableList();
            resetVariable(variableList,workflow);
            StrategyFunction strategy = strategy_map.get(strategyName);
            group.setValue(strategy.apply(variableList));
            nodeVariable.put(group.getField(), group.getValue());
        }
        node.getDetail().put("strategy", strategyName);
        node.getDetail().put("groupList", groupList);
        return new NodeResult(nodeVariable);
    }

    private void resetVariable(List<VariableAggregationNode.Variable> variableList, Workflow workflow) {
        for (VariableAggregationNode.Variable e : variableList) {
            String nodeId = e.getVariable().get(0);
            String field = e.getVariable().get(1);
            INode lfNode = workflow.getNode(nodeId);
            Object value = workflow.getReferenceField(nodeId, field);
            e.setNodeName(lfNode.getProperties().getString("nodeName"));
            e.setField(field);
            e.setValue(value);
        }
    }


    @FunctionalInterface
    public interface StrategyFunction {
        Object apply(List<VariableAggregationNode.Variable> variableList);
    }

    public static Object getFirstNonNull(List<VariableAggregationNode.Variable> variableList) {
        return variableList.stream().map(VariableAggregationNode.Variable::getValue).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static Object getCollection(List<VariableAggregationNode.Variable> variableList) {
        return variableList.stream().map(VariableAggregationNode.Variable::getValue).toList();
    }

}
