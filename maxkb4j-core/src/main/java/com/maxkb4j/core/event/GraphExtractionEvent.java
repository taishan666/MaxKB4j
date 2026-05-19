package com.maxkb4j.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class GraphExtractionEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final List<String> documentIdList;
    private final String chatModelId;
    private final List<String> stateList;

    public GraphExtractionEvent(Object source, String knowledgeId, List<String> documentIdList, String chatModelId, List<String> stateList) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.documentIdList = documentIdList;
        this.chatModelId = chatModelId;
        this.stateList = stateList;
    }
}