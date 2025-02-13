package com.tarzan.maxkb4j.module.application.workflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class Answer {
    String content;
    String viewType;
    String runtimeNodeId;
    String chatRecordId;
    Map<String, Object> childNode;

    public Answer(String content, String viewType, String runtimeNodeId, String chatRecordId, Map<String, Object> childNode) {
        this.content = content;
        this.viewType = viewType;
        this.runtimeNodeId = runtimeNodeId;
        this.chatRecordId = chatRecordId;
        this.childNode = childNode;
    }
}
