package com.tarzan.maxkb4j.core.workflow.node.start.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.chat.ChatParams;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.START;

public class StartNode extends INode {


    public StartNode(JSONObject properties) {
        super(properties);
        this.type=START.getKey();
    }

    @Override
    public NodeResult execute() {
        //todo 获取全局变量
        ChatParams chatParams=super.getChatParams();
        // 获取默认全局变量
        Map<String, Object> globalVariable = getDefaultGlobalVariable(chatParams);
        // 合并全局变量
        globalVariable.putAll(chatParams.getFormData());
        // 构建节点变量
        Map<String, Object> nodeVariable = Map.of(
                "question", chatParams.getMessage(),
                "image", chatParams.getImageList(),
                "document", chatParams.getDocumentList(),
                "audio", chatParams.getAudioList(),
                "other", chatParams.getOtherList()
        );
        return new NodeResult(nodeVariable, globalVariable);
    }

    public Map<String, Object> getDefaultGlobalVariable(ChatParams chatParams) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resultMap.put("history_context", getHistoryContext());
        resultMap.put("chatId", chatParams.getChatId());
        resultMap.put("chat_user_id", IdWorker.get32UUID());
        resultMap.put("chat_user_type", "ANONYMOUS_USER");
        resultMap.put("chat_user", new JSONObject(Map.of("username", "游客")));
        return resultMap;
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("question",context.get("question"));
        detail.put("imageList",context.get("image"));
        detail.put("documentList",context.get("document"));
        detail.put("audioList",context.get("audio"));
        JSONObject config=properties.getJSONObject("config");
        JSONArray globalFields=config.getJSONArray("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String value=globalField.getString("value");
            globalField.put("key",value);
            globalField.put("value",super.getGlobalVariable().getString(value));
        }
        detail.put("global_fields",globalFields);
        return detail;
    }
}
