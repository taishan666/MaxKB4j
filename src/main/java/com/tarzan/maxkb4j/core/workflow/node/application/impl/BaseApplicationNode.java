package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class BaseApplicationNode extends INode {
    private final ApplicationChatService chatService;

    public BaseApplicationNode() {
        this.type = APPLICATION.getKey();
        this.chatService = SpringUtil.getBean(ApplicationChatService.class);
    }


    @Override
    public NodeResult execute() {
        System.out.println(APPLICATION);
        ApplicationNodeParams nodeParams= super.nodeParams.toJavaObject(ApplicationNodeParams.class);
        WorkflowManage workflowManage=super.getWorkflowManage();
        String chatId=chatService.chatOpen(nodeParams.getApplicationId(),runtimeNodeId);
        List<String> questionFields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflowManage.getReferenceField(questionFields.get(0),questionFields.subList(1, questionFields.size()));
        ChatMessageDTO messageDto = ChatMessageDTO.builder()
                .message(question)
                .chatId(chatId)
                .clientId(nodeParams.getApplicationId())
                .clientType(AuthType.APPLICATION.name())
                .sink(sink)
                .reChat(false).build();
        String answer=chatService.chatMessage(messageDto);
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
        detail.put("image_list", context.get("image"));
        detail.put("document_list", context.get("document"));
        detail.put("audio_list", context.get("audio"));
        detail.put("global_fields", properties.get("globalFields"));
        detail.put("application_node_dict", context.get("application_node_dict"));
        return  detail;
    }


}