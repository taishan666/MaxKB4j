package com.tarzan.maxkb4j.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.event.DocumentIndexEvent;
import com.tarzan.maxkb4j.core.event.ParagraphIndexEvent;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeBaseService;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@AllArgsConstructor
public class DataIndexListener {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ParagraphService paragraphService;

    @Async
    @EventListener
    public void handleEvent(DocumentIndexEvent event) {
        System.out.println("收到文档向量化事件消息: " + event.getDocIds());
        EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusByIds(event.getDocIds(), 1, 0);
        for (String docId : event.getDocIds()) {
            List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId,1, event.getStateList());
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

    @Async
    @EventListener
    public void handleEvent(ParagraphIndexEvent event) {
        System.out.println("收到段落向量化事件消息: " + event.getParagraphId());
        EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(event.getKnowledgeId());
        documentService.updateStatusById(event.getDocId(), 1, 0);
        ParagraphEntity paragraph = paragraphService.getById(event.getParagraphId());
        if (Objects.nonNull(paragraph)){
           // log.info("开始--->文档索引:{}", event.getDocId());
            paragraphService.updateStatusById(event.getParagraphId(), 1, 1);
            documentService.updateStatusById(event.getDocId(), 1, 1);
            paragraphService.createIndex(paragraph, embeddingModel);
            paragraphService.updateStatusById(paragraph.getId(),1,2);
            documentService.updateStatusMetaById(event.getDocId());
           // log.info("结束--->文档索引:{}", event.getDocId());
        }
        documentService.updateStatusById(event.getDocId(), 1, 2);
    }
}
