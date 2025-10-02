package com.tarzan.maxkb4j.module.application.domian.vo;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class ChatMessageVO {
    private String chatId;
    private String chatRecordId;
    private String content;
    private String reasoningContent;
    private List<String> upNodeIdList;
    private Boolean operate = true;
    private String nodeId;
    private String realNodeId;
    private String nodeType;
    private String viewType;
    private Boolean nodeIsEnd;
    private String runtimeNodeId;
    private Boolean isEnd;


    public ChatMessageVO(String chatId, String chatRecordId, Boolean isEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = "";
        this.reasoningContent = "";
        this.upNodeIdList = new ArrayList<>();
        this.isEnd = isEnd;
    }

    public ChatMessageVO(String chatId, String chatRecordId, String content, String reasoningContent, String nodeType, String viewType, Boolean nodeIsEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.nodeType = nodeType;
        this.viewType = viewType;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = false;
    }


    public ChatMessageVO(String chatId, String chatRecordId, String nodeId, String content, String reasoningContent, List<String> upNodeIdList,String runtimeNodeId, String nodeType, String viewType, Boolean nodeIsEnd) {
        this.chatId = chatId;
        this.chatRecordId = chatRecordId;
        this.nodeId = nodeId;
        this.realNodeId = nodeId;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.upNodeIdList = upNodeIdList;
        this.runtimeNodeId = runtimeNodeId;
        this.nodeType = nodeType;
        this.viewType = viewType;
        this.nodeIsEnd = nodeIsEnd;
        this.isEnd = false;
    }


}
