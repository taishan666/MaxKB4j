package com.tarzan.maxkb4j.core.workflow.node.directreply.input;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
public class ReplyNodeParams {
    private String replyType;
    private List<String> fields;
    private String content;
    private Boolean isResult;
}
