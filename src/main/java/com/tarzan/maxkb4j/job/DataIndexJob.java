package com.tarzan.maxkb4j.job;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.service.DatasetBaseService;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class DataIndexJob {

    private final DatasetBaseService datasetService;
    private final DocumentService documentService;

    @Scheduled(cron = "0/1 * * * * *")
    public void execute() {
        List<DocumentEntity> indexDocs=documentService.lambdaQuery().select(DocumentEntity::getId, DocumentEntity::getKnowledgeId).likeLeft(DocumentEntity::getStatus, "0").or().likeLeft(DocumentEntity::getStatus, "1").last("limit 5").list();
        indexDocs.parallelStream().forEach(doc -> documentService.createIndexByDocId(datasetService.getDatasetEmbeddingModel(doc.getKnowledgeId()),doc.getId()));
    }
}
