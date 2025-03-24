package com.tarzan.maxkb4j.module.application.vo;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatMessageVO {
    private String chatId;
    private String chatRecordId;
    private String content;
    private Boolean operate;
   // private String nodeId;
    private String nodeType;
    private String viewType;
    private Boolean nodeIsEnd;
    private String runtimeNodeId;
    private Boolean isEnd;




    public ChatMessageVO(String chatId, String content, Boolean isEnd) {
        this.chatId = chatId;
        this.content = content;
        this.isEnd = isEnd;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content, Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.isEnd = isEnd;
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
    }



}
