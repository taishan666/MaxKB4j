package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import com.maxkb4j.common.util.ObjectUtil;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.VariableSplittingNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.VARIABLE_SPLITTING)
@Component
public class VariableSplittingNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        VariableSplittingNode.NodeParams params = parseParams(node, VariableSplittingNode.NodeParams.class);
        List<String> inputVariable = params.getInputVariable();
        Object inputValue = workflow.getReferenceField(inputVariable);

        Map<String, Object> nodeVariable = new HashMap<>();
        List<VariableSplittingNode.Variable> variableList = params.getVariableList();
        Map<String, Object> result = new HashMap<>();

        for (VariableSplittingNode.Variable variable : variableList) {
            String json = inputValue.toString();
            if (!ObjectUtil.isSimpleType(inputValue)) {
                json = JSON.toJSONString(inputValue);
            }
            Object value = JSONPath.eval(json, variable.getExpression());
            result.put(variable.getField(), value == null ? "None" : value);
        }

        nodeVariable.put("result", result);
        nodeVariable.putAll(result);

        putDetail(node, "result", result);

        return new NodeResult(nodeVariable);
    }
}
