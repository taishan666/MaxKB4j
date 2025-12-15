package com.tarzan.maxkb4j.module.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
@Service
@RequiredArgsConstructor
public class ProblemParagraphService extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity>{

    private final ProblemMapper problemMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DataIndexService dataIndexService;

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }
    @Transactional
    public boolean association(String knowledgeId, String docId, String paragraphId, String problemId) {
        ProblemParagraphEntity entity = new ProblemParagraphEntity();
        entity.setKnowledgeId(knowledgeId);
        entity.setProblemId(problemId);
        entity.setParagraphId(paragraphId);
        entity.setDocumentId(docId);
        EmbeddingModel embeddingModel=knowledgeBaseService.getEmbeddingModel(knowledgeId);
        return this.save(entity) && createProblemIndex(knowledgeId, docId, paragraphId, problemId,embeddingModel);
    }

    @Transactional
    public boolean unAssociation(String knowledgeId, String docId, String paragraphId, String problemId) {
        dataIndexService.removeByProblemIdAndParagraphId(knowledgeId,problemId,paragraphId);
        return this.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getKnowledgeId, knowledgeId)
                .remove();
    }

    public boolean createProblemIndex(String knowledgeId, String docId, String paragraphId, String problemId, EmbeddingModel embeddingModel) {
        ProblemEntity problem = problemMapper.selectById(problemId);
        if (Objects.nonNull(problem)) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setKnowledgeId(knowledgeId);
            embeddingEntity.setDocumentId(docId);
            embeddingEntity.setParagraphId(paragraphId);
            embeddingEntity.setSourceId(problemId);
            embeddingEntity.setSourceType(SourceType.PROBLEM);
            embeddingEntity.setIsActive(true);
            embeddingEntity.setContent(problem.getContent());
            dataIndexService.insertAll(List.of(embeddingEntity),embeddingModel);
            return true;
        }
        return false;
    }
}
