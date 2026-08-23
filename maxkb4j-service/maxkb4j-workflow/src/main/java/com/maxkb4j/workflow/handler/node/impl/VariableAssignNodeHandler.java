package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.ValueType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.VariableAssignNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.VARIABLE_ASSIGN)
@Component
public class VariableAssignNodeHandler extends AbsNodeHandler {


    @SuppressWarnings("unchecked")
    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        VariableAssignNode.NodeParams params = parseParams(node, VariableAssignNode.NodeParams.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> variable : params.getVariableList()) {
            if (variable == null || !variable.containsKey(NodeField.FIELDS)) {
                continue;
            }
            List<String> fields = (List<String>) variable.get(NodeField.FIELDS);
            if (fields == null || fields.size() < 2) {
                continue;
            }
            String scope = fields.getFirst();
            if (Scope.GLOBAL.equals(scope)) {
                resultList.add(getGlobalHandleResult(workflow, variable, fields));
            }
            if (Scope.CHAT.equals(scope)) {
                Map<String, Object> chatVariables = getChatHandleResult(workflow, variable, fields);
                resultList.add(chatVariables);
                String chatId = (String) workflow.getGlobalContext().get(ChatField.CHAT_ID);
                if (chatId != null){
                    ChatInfo chatInfo = ChatCache.get(chatId);
                    chatInfo.putChatVariables(chatVariables);
                    ChatCache.put(chatId, chatInfo);
                }
            }
            if (Scope.LOOP.equals(scope)) {
                resultList.add(getLoopHandleResult(workflow, variable, fields));
            }
        }
        putDetail(node, NodeField.RESULT_LIST, resultList);
        return new NodeResult(Map.of());
    }

    private Map<String, Object> getGlobalHandleResult(IWorkflow workflow, Map<String, Object> variable, List<String> fields) {
        String varName = fields.get(1);
        String inputValue = getReferenceContent(workflow, fields);
        Object value = resolveValue(workflow, variable);
        workflow.getGlobalContext().put(varName, value);
        Map<String, Object> result = new HashMap<>();
        result.put(VariableField.NAME, variable.get(VariableField.NAME));
        result.put(VariableField.INPUT_VALUE, inputValue);
        result.put(VariableField.OUTPUT_VALUE, value);
        return result;
    }

    private Map<String, Object> getLoopHandleResult(IWorkflow workflow, Map<String, Object> variable, List<String> fields) {
        Map<String, Object> result = new HashMap<>();
        String varName = fields.get(1);
        String inputValue = getReferenceContent(workflow, fields);
        Object value = resolveValue(workflow, variable);
        workflow.getLoopContext().put(varName, value);
        result.put(VariableField.NAME, variable.get(VariableField.NAME));
        result.put(VariableField.INPUT_VALUE, inputValue);
        result.put(VariableField.OUTPUT_VALUE, value);
        return result;
    }

    private Map<String, Object> getChatHandleResult(IWorkflow workflow, Map<String, Object> variable, List<String> fields) {
        String varName = fields.get(1);
        String inputValue = getReferenceContent(workflow, fields);
        Object value = resolveValue(workflow, variable);
        workflow.getChatContext().put(varName, value);
        Map<String, Object> result = new HashMap<>();
        result.put(VariableField.NAME, variable.get(VariableField.NAME));
        result.put(VariableField.INPUT_VALUE, inputValue);
        result.put(VariableField.OUTPUT_VALUE, value);
        // Update chat variables
        String chatId = (String) workflow.getGlobalContext().get(ChatField.CHAT_ID);
        if (chatId!= null) {
            ChatInfo chatInfo = ChatCache.get(chatId);
            if (chatInfo != null && chatInfo.getChatVariables() != null) {
                chatInfo.getChatVariables().put(varName,value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object resolveValue(IWorkflow workflow, Map<String, Object> variable) {
        String source = (String) variable.get(NodeField.SOURCE);
        if (ValueType.referencing.name().equals(source)) {
            List<String> reference = (List<String>) variable.get(VariableField.REFERENCE);
            if (reference != null && reference.size() >= 2) {
                return workflow.getReferenceField(reference);
            }
        }
        Object value = variable.get(VariableField.VALUE);
        return workflow.renderPrompt(String.valueOf(value));
    }

    public String getReferenceContent(IWorkflow workflow, List<String> fields) {
        if (fields == null || fields.size() < 2) {
            return "";
        }
        Object result = workflow.getReferenceField(fields);
        return result == null ? "" : String.valueOf(result);
    }
}