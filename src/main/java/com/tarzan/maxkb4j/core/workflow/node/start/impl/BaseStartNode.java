package com.tarzan.maxkb4j.core.workflow.node.start.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.dto.ChatRecordSimple;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.START;

public class BaseStartNode extends INode {


    public BaseStartNode() {
        super();
        this.type=START.getKey();
    }

    @Override
    public NodeResult execute() {
        System.out.println(START);
        // 获取基础节点
        LfNode baseNode = workflowManage.getBaseNode();
        // 获取默认全局变量
        List<JSONObject> inputFieldList = (List<JSONObject>) baseNode.getProperties()
                .getOrDefault("inputFieldList", Collections.emptyList());
        JSONObject defaultGlobalVariable = getDefaultGlobalVariable(inputFieldList);
        // 合并全局变量
        Map<String, Object> workflowVariable = new HashMap<>(defaultGlobalVariable);
        workflowVariable.putAll(getGlobalVariable(flowParams, workflowManage));
        // 构建节点变量
        Map<String, Object> nodeVariable = Map.of(
                "question", flowParams.getQuestion(),
                "image", workflowManage.getImageList(),
                "document", workflowManage.getDocumentList(),
                "audio", workflowManage.getAudioList()
        );
        return new NodeResult(nodeVariable, workflowVariable);
    }

    public Map<String, Object> getGlobalVariable(FlowParams workflowParams, WorkflowManage workflowManage) {
        // 获取历史聊天记录
        List<ApplicationChatRecordEntity> historyChatRecord = workflowParams.getHistoryChatRecord();
        List<ChatRecordSimple> historyContext = new ArrayList<>();
        for (ApplicationChatRecordEntity chatRecord : historyChatRecord) {
            ChatRecordSimple record = new ChatRecordSimple();
            record.setQuestion(chatRecord.getProblemText());
            record.setAnswer(chatRecord.getAnswerText());
            historyContext.add(record);
        }
        // 获取chat_id并确保其为字符串形式
        String chatId = workflowParams.getChatId();
        // 构建返回的map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        //resultMap.put("start_time", System.currentTimeMillis());
        resultMap.put("history_context", historyContext);
        resultMap.put("chatId", chatId);
        // 合并node.workflow_manage.form_data
        if (workflowManage != null && workflowManage.getFormData() != null) {
            resultMap.putAll(workflowManage.getFormData());
        }
        return resultMap;
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("question",context.get("question"));
        detail.put("image_list",context.get("image"));
        detail.put("document_list",context.get("document"));
        detail.put("audio_list",context.get("audio"));
        JSONObject config=lfNode.getProperties().getJSONObject("config");
        JSONArray globalFields=config.getJSONArray("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String value=globalField.getString("value");
            globalField.put("key",value);
            globalField.put("value",workflowManage.getContext().getString(value));
        }
        detail.put("global_fields",globalFields);
        return detail;
    }
}
