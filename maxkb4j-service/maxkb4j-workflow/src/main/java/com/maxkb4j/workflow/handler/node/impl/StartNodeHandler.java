package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.cache.ChatCache;
import com.maxkb4j.application.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.ChatRecordSimple;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeHandlerType(NodeType.START)
@Component
public class StartNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        ChatParams chatParams=workflow.getChatParams();
        // 获取默认全局变量
        Map<String, Object> globalVariable = getDefaultGlobalVariable(workflow,chatParams);
        // 合并全局变量
        if(chatParams.getFormData()!=null){
            globalVariable.putAll(chatParams.getFormData());
        }
        workflow.getContext().putAll(globalVariable);
        JSONObject config=node.getProperties().getJSONObject("config");
        JSONArray globalFields=config.getJSONArray("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String key=globalField.getString("value");
            globalField.put("key",key);
            globalField.put("value", workflow.getContext().get(key));
        }
        node.getDetail().put("globalFields",globalFields);
        //会话变量
        workflow.getChatContext().putAll(getChatVariable(node,chatParams.getChatId()));
        // 构建节点变量
        Map<String, Object> nodeVariable =new HashMap<>();
        nodeVariable.put("question", chatParams.getMessage());
        nodeVariable.put("image", chatParams.getImageList());
        nodeVariable.put("document", chatParams.getDocumentList());
        nodeVariable.put("audio", chatParams.getAudioList());
        nodeVariable.put("other", chatParams.getOtherList());
        return new NodeResult(nodeVariable);
    }

    private Map<String, Object> getDefaultGlobalVariable(Workflow workflow,ChatParams chatParams) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resultMap.put("historyContext", getHistoryContext(workflow));
        resultMap.put("chatId", chatParams.getChatId());
        resultMap.put("chatUserId", IdWorker.get32UUID());
        resultMap.put("chatUserType", chatParams.getChatUserType());
        resultMap.put("chatUser", new JSONObject(Map.of("username", "游客")));
        return resultMap;
    }

    private List<ChatRecordSimple> getHistoryContext(Workflow workflow) {
        // 获取历史聊天记录
        List<ChatRecordSimple> list = new ArrayList<>();
        for (ChatRecordDTO chatRecord : workflow.getHistoryChatRecords()) {
            ChatRecordSimple record = new ChatRecordSimple();
            record.setQuestion(chatRecord.getProblemText());
            record.setAnswer(chatRecord.getAnswerText());
            list.add(record);
        }
        return list;
    }

    private Map<String, Object> getChatVariable(AbsNode node, String chatId) {
        Map<String, Object> resultMap = new HashMap<>();
        //更新会话变量
        ChatInfo chatInfo = ChatCache.get(chatId);
        Map<String, Object> chatVariable=chatInfo.getChatVariables();
        JSONObject config=node.getProperties().getJSONObject("config");
        if (config != null){
            JSONArray chatFields=config.getJSONArray("chatFields");
            if (chatFields!=null){
                for (int i = 0; i < chatFields.size(); i++) {
                    JSONObject chatField=chatFields.getJSONObject(i);
                    String key=chatField.getString("value");
                    resultMap.put(key, chatVariable.getOrDefault(key, "None"));
                }
            }
        }
        return resultMap;
    }
}
