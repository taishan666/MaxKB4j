package com.maxkb4j.start.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.core.event.GraphExtractionEvent;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.service.GraphExtractionService;
import com.maxkb4j.knowledge.service.IDocumentService;
import com.maxkb4j.knowledge.service.IParagraphService;
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
public class GraphExtractionListener {

    private final IDocumentService documentService;
    private final IParagraphService paragraphService;
    private final IModelProviderService modelFactory;
    private final KnowledgeModelService knowledgeModelService;
    private final GraphExtractionService graphExtractionService;

    @Async
    @EventListener
    public void handleEvent(GraphExtractionEvent event) {
        log.info("Received graph extraction event for documents: {}", event.getDocumentIdList());

        ChatModel chatModel = modelFactory.buildChatModel(event.getChatModelId());
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());

        for (String docId : event.getDocumentIdList()) {
            try {
                List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId, 2, event.getStateList());
                if (CollectionUtils.isNotEmpty(paragraphs)) {
                    log.info("Starting graph extraction for document: {}", docId);
                    List<String> paragraphIds = paragraphs.stream().map(ParagraphEntity::getId).toList();

                    graphExtractionService.extractFromDocument(
                            chatModel, embeddingModel, event.getKnowledgeId(), docId, paragraphs);

                    log.info("Completed graph extraction for document: {}", docId);
                }
            } catch (Exception e) {
                log.error("Graph extraction failed for document: docId={}, error: {}", docId, e.getMessage(), e);
            }
        }
    }
}