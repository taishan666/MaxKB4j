package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
@Service
public class ProblemParagraphService extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity>{

    @Autowired
    private EmbeddingService embeddingService;

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }

    public boolean association(String datasetId, String docId, String paragraphId, String problemId) {
        ProblemParagraphEntity entity = new ProblemParagraphEntity();
        entity.setDatasetId(datasetId);
        entity.setProblemId(problemId);
        entity.setParagraphId(paragraphId);
        entity.setDocumentId(docId);
        return this.save(entity) && embeddingService.createProblem(datasetId, docId, paragraphId, problemId);
    }

    public boolean unAssociation(String datasetId, String docId, String paragraphId, String problemId) {
        return this.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getDatasetId, datasetId)
                .remove() && embeddingService.lambdaUpdate().eq(EmbeddingEntity::getSourceId, problemId).eq(EmbeddingEntity::getParagraphId, paragraphId).remove();
    }
}
