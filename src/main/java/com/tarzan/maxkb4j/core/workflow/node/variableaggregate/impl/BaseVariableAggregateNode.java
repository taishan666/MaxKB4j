package com.tarzan.maxkb4j.core.workflow.node.variableaggregate.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.variableaggregate.input.VariableAggregateParams;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_AGGREGATE;

public class BaseVariableAggregateNode extends INode {
    public BaseVariableAggregateNode() {
        super();
        this.type = VARIABLE_AGGREGATE.getKey();
    }

    @Override
    public NodeResult execute() {
        VariableAggregateParams nodeParams=super.nodeParams.toJavaObject(VariableAggregateParams.class);
        Object result=workflowManage.getWorkflowContent();
        for (Map<String, Object> variable : nodeParams.getVariableList()) {
            if (!variable.containsKey("fields")) {
                continue;
            }
            List<String> fields = (List<String>) variable.get("fields");
            result= getReferenceContent(fields);
        }
        return new NodeResult(Map.of("variable_list",nodeParams.getVariableList(),"result",result),Map.of());
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
        detail.put("result",context.get("result"));
        return detail;
    }

}
