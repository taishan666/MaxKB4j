package com.tarzan.maxkb4j.module.chat.dto;

import lombok.Data;

@Data
public class ChildNode {
    private String chatRecordId;

    private String runtimeNodeId;

    private ChildNode childNode;

    public ChildNode() {
    }

    public ChildNode(String chatRecordId, String runtimeNodeId) {
        this.chatRecordId = chatRecordId;
        this.runtimeNodeId = runtimeNodeId;
    }
}
