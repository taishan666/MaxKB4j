package com.tarzan.maxkb4j.core.workflow.node.variableassign.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.input.VariableAssignParams;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_ASSIGN;

public class BaseVariableAssignNode extends INode {
    public BaseVariableAssignNode(JSONObject properties) {
        super(properties);
        this.type = VARIABLE_ASSIGN.getKey();
    }

    @Override
    public NodeResult execute() {
        VariableAssignParams nodeParams=super.getNodeData().toJavaObject(VariableAssignParams.class);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (Map<String, Object> variable : nodeParams.getVariableList()) {
            if (!variable.containsKey("fields")) {
                continue;
            }

            List<String> fields = (List<String>) variable.get("fields");
            if ("global".equals(fields.get(0))) {
                Map<String, Object> result = new HashMap<>();
                result.put("name", variable.get("name"));
                result.put("input_value", getReferenceContent((List<String>) variable.get("fields")));

                String source = (String) variable.get("source");
                if ("custom".equals(source)) {
                    String type = (String) variable.get("type");
                    if (Arrays.asList("dict", "array").contains(type)) {
                        Object value = variable.get("value");
                        workflowManage.getContext().put(fields.get(1), value);
                        result.put("output_value", variable.put("value", value));
                    } else {
                        workflowManage.getContext().put(fields.get(1), variable.get("value"));
                        result.put("output_value", variable.get("value"));
                    }
                } else {
                    Object reference = getReferenceContent((List<String>) variable.get("reference"));
                    workflowManage.getContext().put(fields.get(1), reference);
                    result.put("output_value", reference);
                }
                resultList.add(result);
            }
        }
        return new NodeResult(Map.of("variable_list",nodeParams.getVariableList(),"result_list",resultList),Map.of());
    }

    public String getReferenceContent(List<String> fields) {
        if (fields == null || fields.size() < 2) {
            throw new IllegalArgumentException("Fields list must contain at least two elements.");
        }

        // 提取 fields[0] 和 fields[1:]
        String firstField = fields.get(0);
        List<String> remainingFields = fields.subList(1, fields.size());

        // 调用 workflowManage 的 getReferenceField 方法
        Object result = workflowManage.getReferenceField(firstField, remainingFields);

        // 将结果转换为字符串
        return String.valueOf(result);
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("variable_list",context.get("variable_list"));
        detail.put("result_list",context.get("result_list"));
        return detail;
    }

}
