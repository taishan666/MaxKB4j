package com.maxkb4j.knowledge.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.impl.DocumentServiceImpl;
import com.maxkb4j.knowledge.service.impl.ParagraphServiceImpl;
import com.maxkb4j.knowledge.service.impl.ProblemServiceImpl;
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
    private final ProblemServiceImpl problemService;

    @Async
    @EventListener
    public void handleEvent(DocumentIndexEvent event) {
        log.info("收到文档向量化事件消息: {}", event.getDocIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocIds(), 1, 0);
        for (String docId : event.getDocIds()) {
            try {
                List<String> paragraphIds = paragraphService.listParagraphIdsByStates(docId, 1, event.getStateList());
                embedBatch(embeddingModel,event.getKnowledgeId(), docId, paragraphIds);
            } catch (Exception e) {
                log.error("文档索引失败: {}, 错误: {}", docId, e.getMessage(), e);
                // 单个文档失败不影响其他文档继续处理
            }
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