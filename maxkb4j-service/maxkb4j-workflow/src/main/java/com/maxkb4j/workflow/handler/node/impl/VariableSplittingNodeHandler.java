package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.VariableSplittingNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.VARIABLE_SPLITTING)
@Slf4j
@Component
public class VariableSplittingNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        VariableSplittingNode.NodeParams params = parseParams(node, VariableSplittingNode.NodeParams.class);
        List<String> inputVariable = params.getInputVariable();
        Object inputValue = workflow.getReferenceField(inputVariable);
        if (inputValue instanceof String) {
            try {
                inputValue = JSON.parseObject(inputValue.toString());
            } catch (Exception e) {
                log.error("inputValue is not a json string, inputValue: {}", inputValue);
            }
        }
        putDetail(node, NodeField.REQUEST, JSON.toJSONString(inputValue));
        Map<String, Object> nodeVariable = new HashMap<>();
        List<VariableSplittingNode.Variable> variableList = params.getVariableList();
        Map<String, Object> result = new HashMap<>();

        for (VariableSplittingNode.Variable variable : variableList) {
            Object value = JSONPath.eval(inputValue, variable.getExpression());
            value = value == null ? Defaults.NONE : value;
            result.put(variable.getField(), value);
        }

        nodeVariable.put(NodeField.RESULT, result);
        nodeVariable.putAll(result);

        putDetail(node, NodeField.RESULT, result);

        return new NodeResult(nodeVariable);
    }
}
