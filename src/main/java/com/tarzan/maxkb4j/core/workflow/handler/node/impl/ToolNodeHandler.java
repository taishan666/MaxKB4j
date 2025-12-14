package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.common.util.GroovyScriptExecutor;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ToolNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@NodeHandlerType({NodeType.TOOL,NodeType.TOOL_LIB})
@Component
public class ToolNodeHandler implements INodeHandler {
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ToolNode.NodeParams nodeParams = node.getNodeData().toJavaObject(ToolNode.NodeParams.class);
        Map<String, Object> params = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(nodeParams.getInputFieldList())) {
            for (ToolInputField inputField : nodeParams.getInputFieldList()) {
                Object value = workflow.getFieldValue(inputField.getValue(), inputField.getSource());
                params.put(inputField.getName(), value);
            }
        }
        GroovyScriptExecutor scriptExecutor=new GroovyScriptExecutor(nodeParams.getCode(), nodeParams.getInitParams());
        // 执行脚本并返回结果
        Object result = scriptExecutor.execute(params);
        node.getDetail().put("params", params);
        if (nodeParams.getIsResult()){
            node.setAnswerText(result.toString());
        }
        return new NodeResult(Map.of("result", result));
    }
}
