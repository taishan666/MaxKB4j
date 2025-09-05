package com.tarzan.maxkb4j.module.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
@Service
@AllArgsConstructor
public class ProblemParagraphService extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity>{

    private final EmbeddingService embeddingService;
    private final DatasetBaseService datasetService;

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }
    @Transactional
    public boolean association(String datasetId, String docId, String paragraphId, String problemId) {
        ProblemParagraphEntity entity = new ProblemParagraphEntity();
        entity.setDatasetId(datasetId);
        entity.setProblemId(problemId);
        entity.setParagraphId(paragraphId);
        entity.setDocumentId(docId);
        EmbeddingModel embeddingModel=datasetService.getDatasetEmbeddingModel(datasetId);
        return this.save(entity) && embeddingService.createProblemIndex(datasetId, docId, paragraphId, problemId,embeddingModel);
    }

    @Transactional
    public boolean unAssociation(String datasetId, String docId, String paragraphId, String problemId) {
        return this.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getDatasetId, datasetId)
                .remove() && embeddingService.lambdaUpdate().eq(EmbeddingEntity::getSourceId, problemId).eq(EmbeddingEntity::getParagraphId, paragraphId).remove();
    }
}
