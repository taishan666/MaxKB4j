package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.VariableAggregationNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.VARIABLE_AGGREGATE)
@Component
public class VariableAggregationNodeHandler extends AbsNodeHandler {

    private static final Map<String, StrategyFunction> STRATEGY_MAP = new HashMap<>();

    static {
        STRATEGY_MAP.put("first_non_null", VariableAggregationNodeHandler::getFirstNonNull);
        STRATEGY_MAP.put("variable_to_json", VariableAggregationNodeHandler::getCollection);
    }

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        VariableAggregationNode.NodeParams params = parseParams(node, VariableAggregationNode.NodeParams.class);
        String strategyName = params.getStrategy();
        StrategyFunction strategy = STRATEGY_MAP.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown variable aggregation strategy: " + strategyName);
        }
        Map<String, Object> nodeVariable = new HashMap<>();
        List<VariableAggregationNode.Group> groupList = params.getGroupList();
        if (groupList == null) {
            return new NodeResult(nodeVariable);
        }

        for (VariableAggregationNode.Group group : groupList) {
            List<VariableAggregationNode.Variable> variableList = group.getVariableList();
            resetVariable(variableList, workflow);
            group.setValue(strategy.apply(variableList));
            nodeVariable.put(group.getField(), group.getValue());
        }

        putDetails(node, Map.of(
                "strategy", strategyName,
                "groupList", groupList
        ));

        return new NodeResult(nodeVariable);
    }

    private void resetVariable(List<VariableAggregationNode.Variable> variableList, IWorkflow workflow) {
        for (VariableAggregationNode.Variable e : variableList) {
            String nodeId = e.getVariable().getFirst();
            AbsNode lfNode = workflow.getNode(nodeId);
            String nodeName =lfNode==null?"未知节点": lfNode.getProperties().getString(RuntimeDetailField.NODE_NAME);
            e.setNodeName(nodeName == null ? "未知节点" : nodeName);
            String field = e.getVariable().get(1);
            Object value = workflow.getReferenceField(e.getVariable());
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
