package com.tarzan.maxkb4j.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.event.GenerateProblemEvent;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeBaseService;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import com.tarzan.maxkb4j.module.knowledge.service.ProblemService;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
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

    private final DocumentService documentService;
    private final ParagraphService paragraphService;
    private final ModelFactory modelFactory;
    private final ProblemService problemService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Async
    @EventListener
    public void handleEvent(GenerateProblemEvent event) {
        System.out.println("收到事件消息: " + event.getDocumentIdList());
        ChatModel chatModel=modelFactory.buildChatModel(event.getModelId());
        EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(event.getKnowledgeId());
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
