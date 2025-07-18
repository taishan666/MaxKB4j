package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.dataset.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@AllArgsConstructor
public class EmbedTextService {


    private final DocumentService documentService;
    private final DatasetBaseService datasetService;
    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ModelService modelService;


    public boolean batchGenerateRelated(String datasetId, GenerateProblemDTO dto) {
        if (CollectionUtils.isEmpty(dto.getDocumentIdList())) {
            return false;
        }
        paragraphService.updateStatusByDocIds(dto.getDocumentIdList(), 2, 0);
        documentService.updateStatusByIds(dto.getDocumentIdList(), 2, 0);
        documentService.updateStatusMetaByIds(dto.getDocumentIdList());
        DatasetEntity dataset = datasetService.getById(datasetId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModelId());
        EmbeddingModel embeddingModel = modelService.getModelById(dataset.getEmbeddingModelId());
        dto.getDocumentIdList().parallelStream().forEach(docId -> {
            List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
            List<ProblemEntity> allProblems = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
            documentService.updateStatusById(docId, 2, 1);
            paragraphs.forEach(paragraph -> {
                problemService.generateRelated(chatModel, embeddingModel, datasetId, docId, paragraph, allProblems, dto);
                paragraphService.updateStatusById(paragraph.getId(), 2, 2);
                documentService.updateStatusMetaById(paragraph.getDocumentId());
            });
            documentService.updateStatusById(docId, 2, 2);
        });
        return true;
    }


    public boolean paragraphBatchGenerateRelated(String datasetId, String docId, GenerateProblemDTO dto) {
        paragraphService.updateStatusByDocIds(List.of(docId), 2, 0);
        documentService.updateStatusMetaByIds(List.of(docId));
        documentService.updateStatusByIds(List.of(docId), 2, 0);
        DatasetEntity dataset = datasetService.getById(datasetId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModelId());
        EmbeddingModel embeddingModel = modelService.getModelById(dataset.getEmbeddingModelId());
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        List<ProblemEntity> allProblems = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
        documentService.updateStatusById(docId, 2, 1);
        paragraphs.parallelStream().forEach(paragraph -> {
            problemService.generateRelated(chatModel, embeddingModel, datasetId, docId, paragraph, allProblems, dto);
            documentService.updateStatusMetaById(paragraph.getDocumentId());
        });
        documentService.updateStatusById(docId, 2, 2);
        return true;
    }

}
