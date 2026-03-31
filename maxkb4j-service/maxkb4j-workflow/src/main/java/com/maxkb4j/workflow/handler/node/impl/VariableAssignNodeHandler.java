package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.LoopWorkFlow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.model.params.VariableAssignNodeParams;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.VARIABLE_ASSIGN)
@Component
public class VariableAssignNodeHandler extends AbstractNodeHandler<VariableAssignNodeParams> {

    @Override
    protected Class<VariableAssignNodeParams> getParamsClass() {
        return VariableAssignNodeParams.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, VariableAssignNodeParams params) throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> variable : params.getVariableList()) {
            if (variable == null || !variable.containsKey("fields")) {
                continue;
            }
            List<String> fields = (List<String>) variable.get("fields");
            if (fields == null || fields.size() < 2) {
                continue; // invalid fields
            }
            String scope = fields.get(0);
            if ("global".equals(scope)) {
                resultList.add(getGlobalHandleResult(workflow, variable, fields));
            }
            if ("chat".equals(scope)) {
                resultList.add(getChatHandleResult(workflow, variable, fields));
                // Update chat variables
                if (workflow.getConfiguration().getChatParams() != null
                        && workflow.getConfiguration().getChatParams().getChatId() != null) {
                    ChatInfo chatInfo = ChatCache.get(workflow.getConfiguration().getChatParams().getChatId());
                    if (chatInfo != null && chatInfo.getChatVariables() != null) {
                        chatInfo.getChatVariables().putAll(workflow.getChatContext());
                    }
                }
            }
            if ("loop".equals(scope)) {
                resultList.add(getLoopHandleResult(workflow, variable, fields));
            }
        }
        putDetail(node, "resultList", resultList);
        return buildResult(Map.of());
    }

    private Map<String, Object> getGlobalHandleResult(Workflow workflow, Map<String, Object> variable, List<String> fields) {
        String varName = fields.get(1);
        String inputValue = getReferenceContent(workflow, fields);
        Object value = resolveValue(workflow, variable);
        workflow.getContext().put(varName, value);
        Map<String, Object> result = new HashMap<>();
        result.put("name", variable.get("name"));
        result.put("input_value", inputValue);
        result.put("output_value", value);
        return result;
    }

    private Map<String, Object> getLoopHandleResult(Workflow workflow, Map<String, Object> variable, List<String> fields) {
        Map<String, Object> result = new HashMap<>();
        if (workflow instanceof LoopWorkFlow loopWorkflow) {
            String varName = fields.get(1);
            String inputValue = getReferenceContent(workflow, fields);
            Object value = resolveValue(workflow, variable);
            loopWorkflow.getLoopContext().put(varName, value);
            result.put("name", variable.get("name"));
            result.put("input_value", inputValue);
            result.put("output_value", value);
        }
        return result;
    }

    private Map<String, Object> getChatHandleResult(Workflow workflow, Map<String, Object> variable, List<String> fields) {
        String varName = fields.get(1);
        String inputValue = getReferenceContent(workflow, fields);
        Object value = resolveValue(workflow, variable);
        workflow.getChatContext().put(varName, value);
        if (workflow instanceof LoopWorkFlow loopWorkflow) {
            loopWorkflow.getLoopContext().put(varName, value);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("name", variable.get("name"));
        result.put("input_value", inputValue);
        result.put("output_value", value);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object resolveValue(Workflow workflow, Map<String, Object> variable) {
        String source = (String) variable.get("source");
        if ("referencing".equals(source)) {
            List<String> reference = (List<String>) variable.get("reference");
            if (reference != null && reference.size() >= 2) {
                return workflow.getReferenceField(reference);
            }
        }
        return variable.get("value");
    }

    public String getReferenceContent(Workflow workflow, List<String> fields) {
        if (fields == null || fields.size() < 2) {
            return "";
        }
        Object result = workflow.getReferenceField(fields);
        return result == null ? "" : String.valueOf(result);
    }
}