package com.tarzan.maxkb4j.module.application.vo;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatMessageVO {
    private String chatId;
    private String chatRecordId;
    private String content;
    private String reasoning_content;
    private Boolean operate=false;
    private String nodeId;
    private String nodeType;
    private String viewType;
    private Boolean nodeIsEnd;
    private String runtimeNodeId;
    private Boolean isEnd;
    private Integer messageTokens;
    private Integer answerTokens;




    public ChatMessageVO(String chatId, String content, Boolean isEnd) {
        this.chatId = chatId;
        this.content = content;
        this.isEnd = isEnd;
        this.messageTokens = 0;
        this.answerTokens = 0;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content, Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.isEnd = isEnd;
        this.messageTokens = 0;
        this.answerTokens = 0;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content,Boolean nodeIsEnd, Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = isEnd;
        this.messageTokens = 0;
        this.answerTokens = 0;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content, Boolean isEnd,Integer messageTokens, Integer answerTokens) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.isEnd = isEnd;
        this.messageTokens = messageTokens;
        this.answerTokens = answerTokens;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content,String runtimeNodeId,String nodeType,String viewType, Boolean nodeIsEnd, Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.runtimeNodeId = runtimeNodeId;
        this.nodeType = nodeType;
        this.viewType = viewType;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = isEnd;
        this.messageTokens = 0;
        this.answerTokens = 0;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content,String reasoning_content,String runtimeNodeId,String nodeType,String viewType, Boolean nodeIsEnd, Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.reasoning_content = reasoning_content;
        this.runtimeNodeId = runtimeNodeId;
        this.nodeType = nodeType;
        this.viewType = viewType;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = isEnd;
        this.messageTokens = 0;
        this.answerTokens = 0;
    }



}
