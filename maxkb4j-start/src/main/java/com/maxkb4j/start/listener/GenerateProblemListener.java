package com.maxkb4j.start.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.core.event.GenerateProblemEvent;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.service.IProblemService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.chat.ChatModel;
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
public class GenerateProblemListener {

    private final IDocumentService documentService;
    private final IParagraphService paragraphService;
    private final IModelProviderService modelFactory;
    private final IProblemService problemService;
    private final KnowledgeModelService knowledgeModelService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(GenerateProblemEvent event) {
        log.info("收到问题生成事件消息: {}", event.getDocumentIdList());
        ChatModel chatModel=modelFactory.buildChatModel(event.getModelId());
        EmbeddingModel embeddingModel=knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocumentIdList(), 2, 0);
        List<ProblemEntity> knowledgeProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, event.getKnowledgeId()).list();
        for (String docId : event.getDocumentIdList()) {
            try {
                List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId, 2, event.getStateList());
                if (CollectionUtils.isNotEmpty(paragraphs)) {
                    log.info("开始--->文档问题生成:{}", docId);
                    List<String> paragraphIds = paragraphs.stream().map(ParagraphEntity::getId).toList();
                    //重置完成分段数量
                    paragraphService.updateStatusByIds(paragraphIds, 2, 1);
                    documentService.updateStatusById(docId, 2, 1);
                    paragraphs.forEach(paragraph -> {
                        try {
                            String promptTemplate = event.getPrompt().replace("{number}", event.getNumber());
                            problemService.generateRelated(chatModel, embeddingModel, event.getKnowledgeId(), docId, paragraph, knowledgeProblems, promptTemplate);
                            paragraphService.updateStatusById(paragraph.getId(), 2, 2);
                            documentService.updateStatusMetaById(docId);
                        } catch (Exception e) {
                            log.error("段落问题生成失败: paragraphId={}, 错误: {}", paragraph.getId(), e.getMessage(), e);
                        }
                    });
                    log.info("结束--->文档问题生成:{}", docId);
                }
                documentService.updateStatusById(docId, 2, 2);
            } catch (Exception e) {
                log.error("文档问题生成失败: docId={}, 错误: {}", docId, e.getMessage(), e);
            }
        }
    }

}
