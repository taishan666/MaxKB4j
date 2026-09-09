package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.START)
@Component
public class StartNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        Map<String, Object> nodeVariable = new HashMap<>();
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ChatParams chatParams = chatWorkflow.getChatParams();
            // 获取默认全局变量
            Map<String, Object> globalVariable = getDefaultGlobalVariable(chatWorkflow, chatParams);
            workflow.getGlobalContext().putAll(globalVariable);
            // 会话变量
            Map<String, Object> chatVariable = getChatVariable(node, chatParams.getChatId());
            workflow.getChatContext().putAll(chatVariable);
            JSONObject config = node.getProperties().getJSONObject(NodeField.CONFIG);
            if (config != null){
                JSONArray globalFields = config.getJSONArray(ChatField.GLOBAL_FIELDS);
                if (globalFields != null) {
                    for (int i = 0; i < globalFields.size(); i++) {
                        JSONObject globalField = globalFields.getJSONObject(i);
                        String key = globalField.getString(VariableField.VALUE);
                        globalField.put(VariableField.KEY, key);
                        globalField.put(VariableField.VALUE, workflow.getGlobalContext().getOrDefault(key, Defaults.NONE));
                    }
                    putDetail(node, ChatField.GLOBAL_FIELDS, globalFields);
                }
                JSONArray chatFields = config.getJSONArray(ChatField.CHAT_FIELDS);
                if (chatFields != null) {
                    for (int i = 0; i < chatFields.size(); i++) {
                        JSONObject chatField = chatFields.getJSONObject(i);
                        String key = chatField.getString(VariableField.VALUE);
                        chatField.put(VariableField.KEY, key);
                        chatField.put(VariableField.VALUE, workflow.getChatContext().getOrDefault(key,Defaults.NONE));
                    }
                    putDetail(node, ChatField.CHAT_FIELDS, chatFields);
                }

            }
            // 构建节点变量
            nodeVariable.put(NodeField.QUESTION, chatParams.getMessage());
            nodeVariable.put(NodeField.IMAGE, chatParams.getImageList());
            nodeVariable.put(NodeField.DOCUMENT, chatParams.getDocumentList());
            nodeVariable.put(NodeField.AUDIO, chatParams.getAudioList());
            nodeVariable.put(NodeField.OTHER, chatParams.getOtherList());
        }
        return new NodeResult(nodeVariable);
    }

    private Map<String, Object> getDefaultGlobalVariable(IChatWorkflow chatWorkflow, ChatParams chatParams) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(ChatField.TIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resultMap.put(ChatField.HISTORY_CONTEXT, chatWorkflow.getHistoryChatRecords());
        resultMap.put(ChatField.CHAT_ID, chatParams.getChatId());
        resultMap.put(ChatField.CHAT_USER_ID, chatWorkflow.getChatState().getChatUserId());
        resultMap.put(ChatField.CHAT_USER_TYPE, chatWorkflow.getChatState().getChatUserType());
        resultMap.put(ChatField.CHAT_USER, chatWorkflow.getChatState().getChatUser());
        if (chatParams.getFormData() != null){
            resultMap.putAll(chatParams.getFormData());
        }
        return resultMap;
    }

    public Map<String, Object> getChatVariable(AbsNode node, String chatId) {
        Map<String, Object> resultMap = new HashMap<>();
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            return resultMap;
        }
        Map<String, Object> chatVariable = chatInfo.getChatVariables();
        JSONObject config = node.getProperties().getJSONObject(NodeField.CONFIG);
        if (config != null) {
            JSONArray chatFields = config.getJSONArray(ChatField.CHAT_FIELDS);
            if (chatFields != null) {
                for (int i = 0; i < chatFields.size(); i++) {
                    JSONObject chatField = chatFields.getJSONObject(i);
                    String key = chatField.getString(VariableField.VALUE);
                    resultMap.put(key, chatVariable.getOrDefault(key, Defaults.NONE));
                }
            }
        }
        return resultMap;
    }
}
