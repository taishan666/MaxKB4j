package com.tarzan.maxkb4j.job;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.service.DatasetBaseService;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class DataIndexJob {

    private final DatasetBaseService datasetService;
    private final DocumentService documentService;
    private final ParagraphService paragraphService;

    @Scheduled(cron = "0/1 * * * * *")
    public void execute() {
        List<DocumentEntity> indexDocs=documentService.lambdaQuery().select(DocumentEntity::getId, DocumentEntity::getKnowledgeId).likeLeft(DocumentEntity::getStatus, "0").or().likeLeft(DocumentEntity::getStatus, "1").last("limit 1000").list();
        if (CollectionUtils.isNotEmpty(indexDocs)){
            indexDocs.parallelStream().forEach(doc -> documentService.createIndex(datasetService.getDatasetEmbeddingModel(doc.getKnowledgeId()),doc.getId()));
        }else {
            List<ParagraphEntity> indexParagraphs=paragraphService.lambdaQuery().likeLeft(ParagraphEntity::getStatus, "0").or().likeLeft(ParagraphEntity::getStatus, "1").last("limit 1000").list();
            Map<String,List<ParagraphEntity>> KnowledgeGroup=indexParagraphs.stream().collect(Collectors.groupingBy(ParagraphEntity::getKnowledgeId));
            KnowledgeGroup.forEach((knowledgeId,knowledgeParagraphs)->{
                EmbeddingModel embeddingModel=datasetService.getDatasetEmbeddingModel(knowledgeId);
                Map<String,List<ParagraphEntity>> documentGroup=knowledgeParagraphs.stream().collect(Collectors.groupingBy(ParagraphEntity::getDocumentId));
                documentGroup.forEach((docId,documentParagraphs)->{
                    for (ParagraphEntity paragraph : documentParagraphs) {
                        paragraphService.createIndex(paragraph, embeddingModel);
                    }
                    documentService.updateStatusMetaById(docId);
                });
            });
        }

    }
}
