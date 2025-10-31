package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatRecordSimple;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StartNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ChatParams chatParams=workflow.getChatParams();
        // 获取默认全局变量
        Map<String, Object> globalVariable = getDefaultGlobalVariable(workflow,chatParams);
        // 合并全局变量
        globalVariable.putAll(chatParams.getFormData());
        //会话变量
        workflow.getChatContext().putAll(getChatVariable(node,chatParams.getChatId()));
        // 构建节点变量
        Map<String, Object> nodeVariable =new HashMap<>();
        nodeVariable.put("question", chatParams.getMessage());
        nodeVariable.put("image", chatParams.getImageList());
        nodeVariable.put("document", chatParams.getDocumentList());
        nodeVariable.put("audio", chatParams.getAudioList());
        nodeVariable.put("other", chatParams.getOtherList());
        return new NodeResult(nodeVariable, globalVariable);
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
        for (ApplicationChatRecordEntity chatRecord : workflow.getHistoryChatRecords()) {
            ChatRecordSimple record = new ChatRecordSimple();
            record.setQuestion(chatRecord.getProblemText());
            record.setAnswer(chatRecord.getAnswerText());
            list.add(record);
        }
        return list;
    }

    private Map<String, Object> getChatVariable(INode node,String chatId) {
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
