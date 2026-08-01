package com.maxkb4j.knowledge.listener;

import com.maxkb4j.knowledge.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.ParagraphIndexBatcher;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParagraphIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final ParagraphIndexBatcher indexBatcher;

    @Async
    @EventListener
    public void handleEvent(ParagraphIndexEvent event) {
        log.info("收到段落向量化事件消息: {}", event.getParagraphIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        try {
            indexBatcher.indexBatch(embeddingModel, event.getKnowledgeId(), event.getDocId(), event.getParagraphIds());
        } catch (Exception e) {
            log.error("段落索引失败: docId={}, paragraphIds={}, 错误: {}",
                event.getDocId(), event.getParagraphIds(), e.getMessage(), e);
        }
    }
}