package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.model.LoopWorkFlow;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.VariableAssignNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatInfo;
import org.springframework.stereotype.Component;

import java.util.*;

@NodeHandlerType(NodeType.VARIABLE_ASSIGN)
@Component
public class VariableAssignNodeHandler implements INodeHandler {

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        VariableAssignNode.NodeParams nodeParams = node.getNodeData().toJavaObject(VariableAssignNode.NodeParams.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> variable : nodeParams.getVariableList()) {
            if (variable == null || !variable.containsKey("fields")) {
                continue;
            }
            List<String> fields = (List<String>) variable.get("fields");
            if (fields == null || fields.size() < 2) {
                continue; // invalid fields
            }
            String scope = fields.get(0);
            if ("global".equals(scope) ){
                resultList.add(getGlobalHandleResult(workflow,variable,fields));
            }
            if ("chat".equals(scope) ){
                resultList.add(getChatHandleResult(workflow,variable,fields));
                //更新会话变量
                ChatInfo chatInfo = ChatCache.get(workflow.getChatParams().getChatId());
                chatInfo.getChatVariables().putAll(workflow.getChatContext());
            }
            if ("loop".equals(scope) ){
                resultList.add(getLoopHandleResult(workflow,variable,fields));
            }
        }
        node.getDetail().put("resultList",resultList);
        return new NodeResult(Map.of());
    }

    private Map<String, Object> getGlobalHandleResult(Workflow workflow,Map<String, Object> variable,List<String> fields){
        String varName = fields.get(1);
        // 获取 input_value（原始引用内容）
        String inputValue = getReferenceContent(workflow,fields);
        // 解析 value
        Object value = resolveValue(workflow,variable);
        // 更新 global 变量
        workflow.getContext().put(varName,value);
        // 构建 result
        Map<String, Object> result = new HashMap<>();
        result.put("name", variable.get("name"));
        result.put("input_value", inputValue);
        result.put("output_value", value);
        return result;
    }

    private Map<String, Object> getLoopHandleResult(Workflow workflow,Map<String, Object> variable,List<String> fields){
        Map<String, Object> result = new HashMap<>();
        if (workflow instanceof LoopWorkFlow loopWorkflow){
            String varName = fields.get(1);
            // 获取 input_value（原始引用内容）
            String inputValue = getReferenceContent(workflow,fields);
            // 解析 value
            Object value = resolveValue(workflow,variable);
            // 更新 global 变量
            loopWorkflow.getLoopContext().put(varName,value);
            // 构建 result
            result.put("name", variable.get("name"));
            result.put("input_value", inputValue);
            result.put("output_value", value);
        }
        return result;
    }

    private Map<String, Object> getChatHandleResult(Workflow workflow,Map<String, Object> variable,List<String> fields){
        String varName = fields.get(1);
        // 获取 input_value（原始引用内容）
        String inputValue = getReferenceContent(workflow,fields);
        // 解析 value
        Object value = resolveValue(workflow,variable);
        // 更新 chat变量
        workflow.getChatContext().put(varName,value);
        // 更新 loop变量
        if (workflow instanceof LoopWorkFlow loopWorkflow){
            loopWorkflow.getLoopContext().put(varName,value);
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
    private Object resolveValue(Workflow workflow,Map<String, Object> variable) {
        String source = (String) variable.get("source");
        if ("referencing".equals(source)) {
            @SuppressWarnings("unchecked")
            List<String> reference = (List<String>) variable.get("reference");
            if (reference != null && reference.size() >= 2) {
                return workflow.getReferenceField(reference);
            }
        }
        // 默认返回 variable 中的 value 字段（可能是字面量）
        return variable.get("value");
    }

    public String getReferenceContent(Workflow workflow,List<String> fields) {
        if (fields == null || fields.size() < 2) {
            return ""; // 或抛异常，但 execute 中已做过校验，这里可宽松处理
        }
        Object result = workflow.getReferenceField(fields);
        return result == null ? "" : String.valueOf(result);
    }
}
