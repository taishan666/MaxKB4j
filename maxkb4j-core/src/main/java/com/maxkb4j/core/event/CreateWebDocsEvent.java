package com.maxkb4j.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 创建Web文档事件
 * 用于异步处理Web知识库的文档抓取
 *
 * @author tarzan
 */
@Getter
public class CreateWebDocsEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final String sourceUrl;
    private final String selector;

    public CreateWebDocsEvent(Object source, String knowledgeId, String sourceUrl, String selector) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.sourceUrl = sourceUrl;
        this.selector = selector;
    }

}