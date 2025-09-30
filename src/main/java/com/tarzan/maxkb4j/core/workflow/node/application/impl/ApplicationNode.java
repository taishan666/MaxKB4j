package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.ChatParams;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class ApplicationNode extends INode {
    private final ApplicationChatService chatService;

    public ApplicationNode(JSONObject properties) {
        super(properties);
        this.type = APPLICATION.getKey();
        this.chatService = SpringUtil.getBean(ApplicationChatService.class);
    }


    @Override
    public NodeResult execute() {
        ApplicationNodeParams nodeParams= super.getNodeData().toJavaObject(ApplicationNodeParams.class);
        List<String> questionFields=nodeParams.getQuestionReferenceAddress();
        String question= (String)super.getReferenceField(questionFields.get(0),questionFields.get(1));
        ChatParams chatParams=super.getChatParams();
        ChatParams nodeChatParams=ChatParams.builder()
                .message(question)
                .chatId(nodeParams.getApplicationId())
                .runtimeNodeId(super.runtimeNodeId)
                .stream(chatParams.getStream())
                .reChat(chatParams.getReChat())
                .chatUserId(chatParams.getChatUserId())
                .chatUserType(chatParams.getChatUserType())
                .sink(chatParams.getSink())
                .chatRecordId(chatParams.getChatRecordId())
                .appId(nodeParams.getApplicationId())
                .formData(chatParams.getFormData())
                .nodeData(chatParams.getNodeData())
                .debug(chatParams.getDebug())
                .build();
        String answer=chatService.chatMessage(nodeChatParams);
        return new NodeResult(Map.of(
                "result", answer,
                "question", question
        ), Map.of());
    }

    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }


    //todo 获取节点详情
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
        detail.put("globalFields", properties.get("globalFields"));
        detail.put("application_node_dict", context.get("application_node_dict"));
        return  detail;
    }


}