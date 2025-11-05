package com.tarzan.maxkb4j.module.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "子节点对象")
@Data
public class ChildNode {
    @Schema(description = "对话记录ID")
    private String chatRecordId;
    @Schema(description = "运行节点ID")
    private String runtimeNodeId;
    @Schema(description = "子节点对象", implementation = ChildNode.class)
    private ChildNode childNode;


    public ChildNode(String chatRecordId, String runtimeNodeId) {
        this.chatRecordId = chatRecordId;
        this.runtimeNodeId = runtimeNodeId;
    }
}
