package com.tarzan.maxkb4j.core.workflow.node.variableassign.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.variableassign.input.VariableAssignParams;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.VARIABLE_ASSIGN;

public class VariableAssignNode extends INode {
    public VariableAssignNode(JSONObject properties) {
        super(properties);
        this.setType(VARIABLE_ASSIGN.getKey());
    }

    @Override
    public NodeResult execute() {
        VariableAssignParams nodeParams = super.getNodeData().toJavaObject(VariableAssignParams.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (nodeParams.getVariableList() == null) {
            return new NodeResult(Map.of("variable_list", Collections.emptyList(), "result_list", resultList), Map.of());
        }
        for (Map<String, Object> variable : nodeParams.getVariableList()) {
            if (variable == null || !variable.containsKey("fields")) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<String> fields = (List<String>) variable.get("fields");
            if (fields == null || fields.size() < 2) {
                continue; // invalid fields
            }
            String scope = fields.get(0);
            if ("global".equals(scope) ){
                resultList.add(getHandleResult(variable,fields));
            }
            if ("chat".equals(scope) ){
                resultList.add(getHandleResult(variable,fields));
                //更新会话变量
                ChatInfo chatInfo = ChatCache.get(getChatParams().getChatId());
                chatInfo.getChatVariables().putAll(super.getFlowVariables().get(scope));
            }
        }
        return new NodeResult(Map.of("variable_list",nodeParams.getVariableList(),"result_list",resultList),Map.of());
    }


    private Map<String, Object> getHandleResult(Map<String, Object> variable,List<String> fields){
        String scope = fields.get(0);
        String varName = fields.get(1);
        // 获取 input_value（原始引用内容）
        String inputValue = getReferenceContent(fields);
        // 解析 value
        Object value = resolveValue(variable);
        // 更新 flowVariables
        Map<String, Object> scopeMap = super.getFlowVariables().get(scope);
        if (scopeMap != null) {
            scopeMap.put(varName, value);
        }
        // 构建 result
        Map<String, Object> result = new HashMap<>();
        result.put("name", variable.get("name"));
        result.put("input_value", inputValue);
        result.put("output_value", value); // 注意：这里直接放 value，而不是 variable.put 的返回值
        return result;
    }




    /**
     * 根据 variable 配置解析出实际值
     */
    private Object resolveValue(Map<String, Object> variable) {
        String source = (String) variable.get("source");
        if ("referencing".equals(source)) {
            @SuppressWarnings("unchecked")
            List<String> reference = (List<String>) variable.get("reference");
            if (reference != null && reference.size() >= 2) {
                return super.getReferenceField(reference.get(0), reference.get(1));
            }
        }
        // 默认返回 variable 中的 value 字段（可能是字面量）
        return variable.get("value");
    }

    public String getReferenceContent(List<String> fields) {
        if (fields == null || fields.size() < 2) {
            return ""; // 或抛异常，但 execute 中已做过校验，这里可宽松处理
        }
        Object result = super.getReferenceField(fields.get(0), fields.get(1));
        return result == null ? "" : String.valueOf(result);
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("variable_list", detail.get("variable_list"));
        context.put("result_list", detail.get("result_list"));
    }


}
