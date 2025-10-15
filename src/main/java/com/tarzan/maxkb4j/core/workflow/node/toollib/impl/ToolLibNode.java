package com.tarzan.maxkb4j.core.workflow.node.toollib.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.toollib.input.ToolLibNodeParams;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.TOOL_LIB;

public class ToolLibNode extends INode {
    public ToolLibNode(JSONObject properties) {
        super(properties);
        this.setType(TOOL_LIB.getKey());
    }

    @Override
    public NodeResult execute() {
        ToolLibNodeParams nodeParams = super.getNodeData().toJavaObject(ToolLibNodeParams.class);
        Map<String, Object> params = new HashMap<>(5);
        if(nodeParams.getInitParams()!=null){
            params.putAll(nodeParams.getInitParams());
        }
        if (!CollectionUtils.isEmpty(nodeParams.getInputFieldList())) {
            for (ToolInputField inputField : nodeParams.getInputFieldList()) {
                Object value = inputField.getValue();
                if ("reference".equals(inputField.getSource())) {
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> fields = ( List<String>)value;
                        value = super.getReferenceField(fields.get(0), fields.get(1));
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
        detail.put("params", params);
        super.setAnswerText(result.toString());
        return new NodeResult(Map.of("result", result), Map.of());
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }

    @Override
    public JSONObject getRunDetail() {
        return detail;
    }

}
