package com.maxkb4j.knowledge.listener;

import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.ParagraphIndexBatcher;
import com.maxkb4j.knowledge.service.impl.DocumentServiceImpl;
import com.maxkb4j.knowledge.service.impl.ParagraphServiceImpl;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final DocumentServiceImpl documentService;
    private final ParagraphServiceImpl paragraphService;
    private final ParagraphIndexBatcher indexBatcher;

    @Async
    @EventListener
    public void handleEvent(DocumentIndexEvent event) {
        log.info("收到文档向量化事件消息: {}", event.getDocIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocIds(), 1, 0);
        for (String docId : event.getDocIds()) {
            try {
                List<String> paragraphIds = paragraphService.listParagraphIdsByStates(docId, 1, event.getStateList());
                indexBatcher.indexBatch(embeddingModel, event.getKnowledgeId(), docId, paragraphIds);
            } catch (Exception e) {
                log.error("文档索引失败: {}, 错误: {}", docId, e.getMessage(), e);
                // 单个文档失败不影响其他文档继续处理
            }
        }
    }
}