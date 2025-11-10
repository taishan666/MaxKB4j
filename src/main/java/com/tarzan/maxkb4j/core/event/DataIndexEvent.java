package com.tarzan.maxkb4j.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class DataIndexEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final List<String> docIds;
    private final List<String> stateList;

    public DataIndexEvent(Object source,String knowledgeId, List<String> docIds, List<String> stateList) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.docIds = docIds;
        this.stateList = stateList;
    }

}
