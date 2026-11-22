package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.INode;
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
        STRATEGY_MAP.put(VariableStrategy.FIRST_NON_NULL, VariableAggregationNodeHandler::getFirstNonNull);
        STRATEGY_MAP.put(VariableStrategy.VARIABLE_TO_JSON, VariableAggregationNodeHandler::getCollection);
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
                NodeField.STRATEGY, strategyName,
                NodeField.GROUP_LIST, groupList
        ));

        return new NodeResult(nodeVariable);
    }

    private void resetVariable(List<VariableAggregationNode.Variable> variableList, IWorkflow workflow) {
        if (variableList == null) {
            return;
        }
        for (VariableAggregationNode.Variable e : variableList) {
            // 引用必须为 [nodeId, field] 结构，不完整时跳过重置，避免 NPE/IOOBE
            List<String> reference = e.getVariable();
            if (reference == null || reference.size() < 2) {
                continue;
            }
            String nodeId = reference.getFirst();
            INode node = workflow.getNode(nodeId);
            String nodeName = node == null ? "未知节点" : node.getNodeName();
            e.setNodeName(nodeName == null ? "未知节点" : nodeName);
            String field = reference.get(1);
            Object value = workflow.getReferenceField(reference);
            e.setField(field);
            e.setValue(value);
        }
    }

    @FunctionalInterface
    public interface StrategyFunction {
        Object apply(List<VariableAggregationNode.Variable> variableList);
    }
}
