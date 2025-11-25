package com.tarzan.maxkb4j.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ParagraphIndexEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final String docId;
    private final List<String> paragraphIds;


    public ParagraphIndexEvent(Object source, String knowledgeId,String docId, List<String> paragraphIds) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.docId = docId;
        this.paragraphIds = paragraphIds;
    }

}
