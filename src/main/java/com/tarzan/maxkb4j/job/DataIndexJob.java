package com.tarzan.maxkb4j.job;

import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeBaseService;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class DataIndexJob {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ParagraphService paragraphService;

/*    @Scheduled(cron = "0/1 * * * * *")
    public void execute() {
        List<DocumentEntity> indexDocs=documentService.lambdaQuery().select(DocumentEntity::getId, DocumentEntity::getKnowledgeId).likeLeft(DocumentEntity::getStatus, "0").or().likeLeft(DocumentEntity::getStatus, "1").last("limit 1000").list();
        if (CollectionUtils.isNotEmpty(indexDocs)){
            Map<String,List<DocumentEntity>> KnowledgeGroup=indexDocs.stream().collect(Collectors.groupingBy(DocumentEntity::getKnowledgeId));
            KnowledgeGroup.forEach((knowledgeId,knowledgeDocuments)->{
                EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(knowledgeId);
                for (DocumentEntity doc : knowledgeDocuments) {
                    log.info("开始--->文档索引:{}", doc.getId());
                    List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, doc.getId()).list();
                    documentService.updateStatusById(doc.getId(), 1, 1);
                    paragraphs.forEach(paragraph -> {
                        paragraphService.createIndex(paragraph, embeddingModel);
                        documentService.updateStatusMetaById(doc.getId());
                    });
                    documentService.updateStatusById(doc.getId(), 1, 2);
                    log.info("结束--->文档索引:{}", doc.getId());
                }
            });
        }else {
            List<ParagraphEntity> indexParagraphs=paragraphService.lambdaQuery().likeLeft(ParagraphEntity::getStatus, "0").or().likeLeft(ParagraphEntity::getStatus, "1").last("limit 1000").list();
            Map<String,List<ParagraphEntity>> KnowledgeGroup=indexParagraphs.stream().collect(Collectors.groupingBy(ParagraphEntity::getKnowledgeId));
            KnowledgeGroup.forEach((knowledgeId,knowledgeParagraphs)->{
                EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(knowledgeId);
                Map<String,List<ParagraphEntity>> documentGroup=knowledgeParagraphs.stream().collect(Collectors.groupingBy(ParagraphEntity::getDocumentId));
                documentGroup.forEach((docId,documentParagraphs)->{
                    for (ParagraphEntity paragraph : documentParagraphs) {
                        paragraphService.createIndex(paragraph, embeddingModel);
                    }
                    documentService.updateStatusMetaById(docId);
                });
            });
        }

    }*/
}
