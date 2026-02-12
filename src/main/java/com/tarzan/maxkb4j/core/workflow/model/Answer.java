package com.tarzan.maxkb4j.core.workflow.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Answer {
    private String viewType;
    private String content;
    private String reasoningContent;
    private String chatRecordId;
    private String runtimeNodeId;
    private String realNodeId;
}
