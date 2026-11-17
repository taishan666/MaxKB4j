package com.maxkb4j.knowledge.listener;

import com.maxkb4j.knowledge.event.ParagraphIndexEvent;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.ParagraphIndexBatchExecutor;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParagraphIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final ParagraphIndexBatchExecutor indexBatchExecutor;

    /**
     * 事务提交后才执行，避免异步任务查询不到未提交数据；事务回滚则不执行，
     * 防止对已回滚数据建索引产生脏数据。发布方无事务时立即执行。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(ParagraphIndexEvent event) {
        log.info("收到段落向量化事件消息: {}", event.getParagraphIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        try {
            indexBatchExecutor.indexBatch(embeddingModel, event.getKnowledgeId(), event.getDocId(), event.getParagraphIds());
        } catch (Exception e) {
            log.error("段落索引失败: docId={}, paragraphIds={}, 错误: {}",
                    event.getDocId(), event.getParagraphIds(), e.getMessage(), e);
        }
    }
}