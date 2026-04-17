package com.maxkb4j.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class GenerateProblemEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final List<String> documentIdList;
    private final String modelId;
    private final String number;
    private final String prompt;
    private final List<String> stateList;

    public GenerateProblemEvent(Object source, String knowledgeId,List<String> documentIdList, String modelId,String number, String prompt, List<String> stateList) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.documentIdList = documentIdList;
        this.modelId = modelId;
        this.number = number;
        this.prompt = prompt;
        this.stateList = stateList;
    }
}
