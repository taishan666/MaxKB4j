package com.tarzan.maxkb4j.listener;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.event.DataIndexEvent;
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

@Slf4j
@Component
@AllArgsConstructor
public class DataIndexListener {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ParagraphService paragraphService;

    @Async
    @EventListener
    public void handleEvent(DataIndexEvent event) {
        EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(event.getKnowledgeId());
        System.out.println("收到事件消息: " + event.getDocIds());
        documentService.updateStatusByIds(event.getDocIds(), 1, 0);
        for (String docId : event.getDocIds()) {
            List<ParagraphEntity> paragraphs = paragraphService.listByStateIds(docId,1, event.getStateList());
            if (CollectionUtils.isNotEmpty(paragraphs)){
                log.info("开始--->文档索引:{}", docId);
                List<String> paragraphIds = paragraphs.stream().map(ParagraphEntity::getId).toList();
                //重置完成分段数量
                paragraphService.updateStatusByIds(paragraphIds, 1, 0);
                documentService.updateStatusById(docId, 1, 1);
                paragraphs.forEach(paragraph -> {
                    paragraphService.createIndex(paragraph, embeddingModel);
                    documentService.updateStatusMetaById(docId);
                });
                log.info("结束--->文档索引:{}", docId);
            }
            documentService.updateStatusById(docId, 1, 2);
        }
    }
}
