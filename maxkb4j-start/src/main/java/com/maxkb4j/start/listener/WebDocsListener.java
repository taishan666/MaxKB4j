package com.maxkb4j.start.listener;

import com.maxkb4j.core.event.CreateWebDocsEvent;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.service.DocumentService;
import com.maxkb4j.knowledge.service.DocumentWebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Web文档创建监听器
 * 处理Web知识库文档的异步抓取
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebDocsListener {

    private final DocumentWebService documentWebService;
    private final DocumentService documentService;

    @Async
    @Transactional
    @EventListener
    public void handleEvent(CreateWebDocsEvent event) {
        log.info("收到Web文档创建事件: knowledgeId={}, sourceUrl={}", event.getKnowledgeId(), event.getSourceUrl());
        try {
            List<DocumentSimple> docs = documentWebService.getWebDocuments(
                event.getSourceUrl(),
                event.getSelector(),
                true
            );
            documentService.batchCreateDocs(event.getKnowledgeId(), KnowledgeType.WEB, docs);
            log.info("Web文档创建完成: knowledgeId={}, 文档数量={}", event.getKnowledgeId(), docs.size());
        } catch (Exception e) {
            log.error("Web文档创建失败: knowledgeId={}, sourceUrl={}, 错误: {}",
                event.getKnowledgeId(), event.getSourceUrl(), e.getMessage(), e);
        }
    }

}