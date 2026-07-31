package com.maxkb4j.knowledge.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
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
public class ParagraphIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final DocumentServiceImpl documentService;
    private final ParagraphServiceImpl paragraphService;

    @Async
    @EventListener
    public void handleEvent(ParagraphIndexEvent event) {
        log.info("收到段落向量化事件消息: {}", event.getParagraphIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        try {
            embedBatch(embeddingModel,event.getKnowledgeId(), event.getDocId(), event.getParagraphIds());
        } catch (Exception e) {
            log.error("段落索引失败: docId={}, paragraphIds={}, 错误: {}",
                event.getDocId(), event.getParagraphIds(), e.getMessage(), e);
        }
    }

    /**
     * Batch embed paragraphs with optimized processing
     */
    private void embedBatch(EmbeddingModel embeddingModel,String knowledgeId,  String docId, List<String> paragraphIds) {
        documentService.updateStatusById(docId, 1, 0);

        if (CollectionUtils.isNotEmpty(paragraphIds)) {
            log.info("开始--->文档索引: {}", docId);
            documentService.updateStatusById(docId, 1, 1);

            paragraphService.updateStatusByIds(paragraphIds, 1, 0);

            try {
                // Use batch indexing instead of processing one by one
                paragraphService.createIndexBatch(knowledgeId,paragraphIds, embeddingModel);

                // Update all paragraph statuses to completed
                paragraphService.updateStatusByIds(paragraphIds, 1, 2);

                // Update document status
                documentService.updateStatusMetaById(docId);

                log.info("结束--->文档索引: {} (处理了 {} 个段落)", docId, paragraphIds.size());

            } catch (Exception e) {
                log.error("文档索引失败: {}, 错误: {}", docId, e.getMessage(), e);
                // Keep paragraphs in processing state for retry
                throw new RuntimeException("文档索引失败: " + docId, e);
            }
        }

        documentService.updateStatusById(docId, 1, 2);
    }

}