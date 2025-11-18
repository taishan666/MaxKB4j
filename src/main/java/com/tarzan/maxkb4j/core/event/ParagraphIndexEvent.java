package com.tarzan.maxkb4j.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ParagraphIndexEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final String docId;
    private final String paragraphId;


    public ParagraphIndexEvent(Object source, String knowledgeId,String docId, String paragraphId) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.docId = docId;
        this.paragraphId = paragraphId;
    }

}
