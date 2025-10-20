package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.tool.input.ToolNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ToolNodeParams nodeParams=node.getNodeData().toJavaObject(ToolNodeParams.class);
        Map<String, Object> params = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(nodeParams.getInputFieldList())) {
            for (ToolInputField inputField : nodeParams.getInputFieldList()) {
                Object value = inputField.getValue();
                if ("reference".equals(inputField.getSource())) {
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> fields = ( List<String>)value;
                        value = workflow.getReferenceField(fields.get(0), fields.get(1));
                    }
                }
                params.put(inputField.getName(), value);
            }
        }
        Binding binding = new Binding(params);
        // 创建 GroovyShell 并执行脚本
        GroovyShell shell = new GroovyShell(binding);
        // 执行脚本并返回结果
        Object result = shell.evaluate(nodeParams.getCode());
        node.getDetail().put("params", params);
        node.setAnswerText(result.toString());
        return new NodeResult(Map.of( "result", result), Map.of());
    }
}
