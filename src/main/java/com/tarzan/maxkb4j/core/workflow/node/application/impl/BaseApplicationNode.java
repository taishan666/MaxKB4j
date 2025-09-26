package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class BaseApplicationNode extends INode {
    private final ApplicationChatService chatService;

    public BaseApplicationNode(JSONObject properties) {
        super(properties);
        this.type = APPLICATION.getKey();
        this.chatService = SpringUtil.getBean(ApplicationChatService.class);
    }


    @Override
    public NodeResult execute() {
        ApplicationNodeParams nodeParams= super.getNodeData().toJavaObject(ApplicationNodeParams.class);
        WorkflowManage workflowManage=super.getWorkflowManage();
        String chatId=chatService.chatOpen(nodeParams.getApplicationId(),runtimeNodeId);
        List<String> questionFields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflowManage.getReferenceField(questionFields.get(0),questionFields.get(1));
        ChatParams chatParams = ChatParams.builder()
                .message(question)
                .chatId(chatId)
                .userId("")
                .sink(workflowManage.getSink())
                .reChat(false).build();
        String answer=chatService.chatMessage(chatParams,true);
        return new NodeResult(Map.of(
                "result", answer,
                "question", question
        ), Map.of());
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("info", properties.getString("nodeData"));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        detail.put("imageList", context.get("image"));
        detail.put("documentList", context.get("document"));
        detail.put("audioList", context.get("audio"));
        detail.put("global_fields", properties.get("globalFields"));
        detail.put("application_node_dict", context.get("application_node_dict"));
        return  detail;
    }


}