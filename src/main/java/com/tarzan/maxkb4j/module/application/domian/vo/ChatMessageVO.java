package com.tarzan.maxkb4j.module.application.domian.vo;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatMessageVO {
    private String chatId;
    private String chatRecordId;
    private String content;
    private String reasoningContent;
   // private Boolean operate=false;
    private String nodeId;
    private String nodeType;
    private String viewType;
    private Boolean nodeIsEnd;
    private String runtimeNodeId;
    private Boolean isEnd;



    public ChatMessageVO(String chatId, String chatRecordId,Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = "";
        this.reasoningContent = "";
        this.nodeIsEnd = false;
        this.isEnd = isEnd;
    }

    public ChatMessageVO(String chatId, String chatRecordId,String content,String reasoningContent, Boolean nodeIsEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = false;
    }



  /*  public ChatMessageVO(String chatId, String chatRecordId,String content,String runtimeNodeId,String nodeType,String viewType, Boolean nodeIsEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.runtimeNodeId = runtimeNodeId;
        this.nodeType = nodeType;
        this.viewType = viewType;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = false;
    }*/

    public ChatMessageVO(String chatId, String chatRecordId,String content,String reasoningContent,String runtimeNodeId,String nodeType,String viewType, Boolean nodeIsEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.runtimeNodeId = runtimeNodeId;
        this.nodeType = nodeType;
        this.viewType = viewType;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = false;
    }



}
