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
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    @EventListener
    public void handleEvent(GenerateProblemEvent event) {
        log.info("收到事件消息: {}", event.getDocumentIdList());
        ChatModel chatModel=modelFactory.buildChatModel(event.getModelId());
        EmbeddingModel embeddingModel=knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocumentIdList(), 2, 0);
        List<ProblemEntity> knowledgeProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, event.getKnowledgeId()).list();
        for (String docId : event.getDocumentIdList()) {
            List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId,2, event.getStateList());
            if (CollectionUtils.isNotEmpty(paragraphs)){
                log.info("开始--->文档问题生成:{}", docId);
                List<String> paragraphIds = paragraphs.stream().map(ParagraphEntity::getId).toList();
                //重置完成分段数量
                paragraphService.updateStatusByIds(paragraphIds, 2, 1);
                documentService.updateStatusById(docId, 2, 1);
                paragraphs.forEach(paragraph -> {
                    problemService.generateRelated(chatModel, embeddingModel, event.getKnowledgeId(), docId, paragraph, knowledgeProblems, event.getPrompt());
                    paragraphService.updateStatusById(paragraph.getId(), 2, 2);
                    documentService.updateStatusMetaById(docId);
                });
                log.info("结束--->文档问题生成:{}", docId);
            }
            documentService.updateStatusById(docId, 2, 2);
        }
    }

}
