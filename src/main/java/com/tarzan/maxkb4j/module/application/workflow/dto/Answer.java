package com.tarzan.maxkb4j.module.application.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class Answer {
    String viewType;
    String content;
    String runtimeNodeId;
    String chatRecordId;
    Map<String, Object> childNode;

    public Answer(String viewType, String content, String runtimeNodeId, String chatRecordId, Map<String, Object> childNode) {
        this.viewType = viewType;
        this.content = content;
        this.runtimeNodeId = runtimeNodeId;
        this.chatRecordId = chatRecordId;
        this.childNode = childNode;
    }
}
