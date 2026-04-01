package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.VariableAggregationNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@NodeHandlerType(NodeType.VARIABLE_AGGREGATE)
@Component
public class VariableAggregationNodeHandler extends AbstractNodeHandler {

    private static final Map<String, StrategyFunction> STRATEGY_MAP = new HashMap<>();

    static {
        STRATEGY_MAP.put("first_non_null", VariableAggregationNodeHandler::getFirstNonNull);
        STRATEGY_MAP.put("variable_to_json", VariableAggregationNodeHandler::getCollection);
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        VariableAggregationNode.NodeParams params = parseParams(node, VariableAggregationNode.NodeParams.class);
        String strategyName = params.getStrategy();
        Map<String, Object> nodeVariable = new HashMap<>();
        List<VariableAggregationNode.Group> groupList = params.getGroupList();

        for (VariableAggregationNode.Group group : groupList) {
            List<VariableAggregationNode.Variable> variableList = group.getVariableList();
            resetVariable(variableList, workflow);
            StrategyFunction strategy = STRATEGY_MAP.get(strategyName);
            group.setValue(strategy.apply(variableList));
            nodeVariable.put(group.getField(), group.getValue());
        }

        putDetails(node, Map.of(
                "strategy", strategyName,
                "groupList", groupList
        ));

        return new NodeResult(nodeVariable);
    }

    private void resetVariable(List<VariableAggregationNode.Variable> variableList, Workflow workflow) {
        for (VariableAggregationNode.Variable e : variableList) {
            String nodeId = e.getVariable().get(0);
            String field = e.getVariable().get(1);
            AbsNode lfNode = workflow.getNode(nodeId);
            Object value = workflow.getReferenceField(e.getVariable());
            String nodeName = lfNode.getProperties().getString("nodeName");
            e.setNodeName(nodeName == null ? "未知" : nodeName);
            e.setField(field);
            e.setValue(value);
        }
    }

    @FunctionalInterface
    public interface StrategyFunction {
        Object apply(List<VariableAggregationNode.Variable> variableList);
    }

    public static Object getFirstNonNull(List<VariableAggregationNode.Variable> variableList) {
        return variableList.stream()
                .map(VariableAggregationNode.Variable::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static Object getCollection(List<VariableAggregationNode.Variable> variableList) {
        return variableList.stream()
                .map(VariableAggregationNode.Variable::getValue)
                .toList();
    }
}
