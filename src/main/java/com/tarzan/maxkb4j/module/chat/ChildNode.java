package com.tarzan.maxkb4j.module.chat;

import lombok.Data;

@Data
public class ChildNode {
    private String chatRecordId;

    private String runtimeNodeId;

    private ChildNode childNode;
}
