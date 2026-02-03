package com.tarzan.maxkb4j.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.event.DocumentIndexEvent;
import com.tarzan.maxkb4j.core.event.ParagraphIndexEvent;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeModelService;
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
public class DataIndexListener {

    private final KnowledgeModelService knowledgeModelService;
    private final DocumentService documentService;
    private final ParagraphService paragraphService;

    @Async
    @EventListener
    public void handleEvent(DocumentIndexEvent event) {
        log.info("收到文档向量化事件消息: {}", event.getDocIds());
        EmbeddingModel embeddingModel=knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocIds(), 1, 0);
        for (String docId : event.getDocIds()) {
            List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId,1, event.getStateList());
            embed(embeddingModel, docId, paragraphs);
        }
    }

    @Async
    @EventListener
    public void handleEvent(ParagraphIndexEvent event) {
        log.info("收到段落向量化事件消息: {}", event.getParagraphIds());
        List<ParagraphEntity> paragraphs= paragraphService.listByIds(event.getParagraphIds());
        EmbeddingModel embeddingModel=knowledgeModelService.getEmbeddingModel(event.getKnowledgeId());
        embed(embeddingModel, event.getDocId(), paragraphs);
    }

    private void embed(EmbeddingModel embeddingModel,String docId,List<ParagraphEntity> paragraphs) {
        documentService.updateStatusById(docId, 1, 0);
        if (CollectionUtils.isNotEmpty(paragraphs)){
            log.info("开始--->文档索引:{}", docId);
            List<String> paragraphIds = paragraphs.stream().map(ParagraphEntity::getId).toList();
            paragraphService.updateStatusByIds(paragraphIds, 1, 1);
            documentService.updateStatusById(docId, 1, 1);
            paragraphs.forEach(paragraph -> {
                paragraphService.createIndex(paragraph, embeddingModel);
                paragraphService.updateStatusById(paragraph.getId(),1,2);
                documentService.updateStatusMetaById(docId);
            });
            log.info("结束--->文档索引:{}", docId);
        }
        documentService.updateStatusById(docId, 1, 2);
    }
}
