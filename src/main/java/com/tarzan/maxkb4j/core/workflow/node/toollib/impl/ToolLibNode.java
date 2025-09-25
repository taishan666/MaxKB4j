package com.tarzan.maxkb4j.core.workflow.node.toollib.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
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
        this.type = TOOL_LIB.getKey();
    }

    @Override
    public NodeResult execute() {
        System.out.println(TOOL_LIB);
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
                        value = workflowManage.getReferenceField(fields.get(0), fields.get(1));
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
        return new NodeResult(Map.of("answer", "", "params", params, "result", result), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("result", context.get("result"));
        detail.put("params", context.get("params"));
        return detail;
    }

}
