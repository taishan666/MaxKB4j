package com.maxkb4j.start.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.core.event.DocumentIndexEvent;
import com.maxkb4j.core.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final IDocumentService documentService;
    private final IParagraphService paragraphService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(DocumentIndexEvent event) {
        log.info("收到文档向量化事件消息: {}", event.getDocIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocIds(), 1, 0);

        for (String docId : event.getDocIds()) {
            try {
                List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId, 1, event.getStateList());
                embedBatch(embeddingModel, docId, paragraphs);
            } catch (Exception e) {
                log.error("文档索引失败: {}, 错误: {}", docId, e.getMessage(), e);
                // 单个文档失败不影响其他文档继续处理
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(ParagraphIndexEvent event) {
        log.info("收到段落向量化事件消息: {}", event.getParagraphIds());
        List<ParagraphEntity> paragraphs = paragraphService.listByIds(event.getParagraphIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        try {
            embedBatch(embeddingModel, event.getDocId(), paragraphs);
        } catch (Exception e) {
            log.error("段落索引失败: docId={}, paragraphIds={}, 错误: {}",
                event.getDocId(), event.getParagraphIds(), e.getMessage(), e);
        }
    }

    /**
     * Batch embed paragraphs with optimized processing
     */
    private void embedBatch(EmbeddingModel embeddingModel, String docId, List<ParagraphEntity> paragraphs) {
        documentService.updateStatusById(docId, 1, 0);

        if (CollectionUtils.isNotEmpty(paragraphs)) {
            log.info("开始--->文档索引: {}", docId);
            documentService.updateStatusById(docId, 1, 1);

            List<String> paragraphIds = paragraphs.stream().map(ParagraphEntity::getId).toList();
            paragraphService.updateStatusByIds(paragraphIds, 1, 0);

            try {
                // Use batch indexing instead of processing one by one
                paragraphService.createIndexBatch(paragraphs, embeddingModel);

                // Update all paragraph statuses to completed
                paragraphService.updateStatusByIds(paragraphIds, 1, 2);

                // Update document status
                documentService.updateStatusMetaById(docId);

                log.info("结束--->文档索引: {} (处理了 {} 个段落)", docId, paragraphs.size());

            } catch (Exception e) {
                log.error("文档索引失败: {}, 错误: {}", docId, e.getMessage(), e);
                // Keep paragraphs in processing state for retry
                throw new RuntimeException("文档索引失败: " + docId, e);
            }
        }

        documentService.updateStatusById(docId, 1, 2);
    }
}