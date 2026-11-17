package com.maxkb4j.knowledge.listener;

import com.maxkb4j.knowledge.consts.TaskStatus;
import com.maxkb4j.knowledge.consts.TaskType;
import com.maxkb4j.knowledge.event.DocumentIndexEvent;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.service.ParagraphIndexBatchExecutor;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final IDocumentInternalService documentService;
    private final IParagraphInternalService paragraphService;
    private final ParagraphIndexBatchExecutor indexBatchExecutor;

    /**
     * 事务提交后才执行，避免异步任务查询不到未提交数据；事务回滚则不执行，
     * 防止对已回滚数据建索引产生脏数据。发布方无事务时（如重新向量化）立即执行。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEvent(DocumentIndexEvent event) {
        log.info("收到文档向量化事件消息: {}", event.getDocIds());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocIds(), TaskType.EMBEDDING, TaskStatus.PENDING);
        for (String docId : event.getDocIds()) {
            try {
                List<String> paragraphIds = paragraphService.listParagraphIdsByStates(docId, 1, event.getStateList());
                indexBatchExecutor.indexBatch(embeddingModel, event.getKnowledgeId(), docId, paragraphIds);
            } catch (Exception e) {
                log.error("文档索引失败: {}, 错误: {}", docId, e.getMessage(), e);
                // 单个文档失败不影响其他文档继续处理
            }
        }
    }
}