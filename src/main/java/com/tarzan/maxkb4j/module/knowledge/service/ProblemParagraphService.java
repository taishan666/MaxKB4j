package com.tarzan.maxkb4j.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.knowledge.consts.SourceType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ProblemParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
@Service
@RequiredArgsConstructor
public class ProblemParagraphService extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity>{

    private final ProblemMapper problemMapper;
    private final KnowledgeModelService knowledgeModelService;
    private final IChunkIndexService chunkIndexService;

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }

    @Transactional
    public boolean association(String knowledgeId, String docId, String paragraphId, String problemId) {
        ProblemParagraphVO problemParagraph = new ProblemParagraphVO();
        problemParagraph.setKnowledgeId(knowledgeId);
        problemParagraph.setProblemId(problemId);
        problemParagraph.setParagraphId(paragraphId);
        problemParagraph.setDocumentId(docId);
        LambdaQueryWrapper<ProblemEntity> wrapper= Wrappers.<ProblemEntity>lambdaQuery().select(ProblemEntity::getContent).eq(ProblemEntity::getId,problemId);
        ProblemEntity  problem= problemMapper.selectOne(wrapper);
        problemParagraph.setContent(problem.getContent());
        EmbeddingModel embeddingModel=knowledgeModelService.getEmbeddingModel(knowledgeId);
        return this.save(problemParagraph) && createProblemsIndex(List.of(problemParagraph),embeddingModel);
    }

    @Transactional
    public boolean unAssociation(String knowledgeId, String docId, String paragraphId, String problemId) {
        chunkIndexService.removeByProblemIdAndParagraphId(knowledgeId,problemId,paragraphId);
        return this.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getKnowledgeId, knowledgeId)
                .remove();
    }

    public boolean createProblemsIndex(List<ProblemParagraphVO> associations , EmbeddingModel embeddingModel) {
        List<EmbeddingEntity> embeddingEntities=associations.stream().map(e -> {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setKnowledgeId(e.getKnowledgeId());
            embeddingEntity.setDocumentId(e.getDocumentId());
            embeddingEntity.setParagraphId(e.getParagraphId());
            embeddingEntity.setSourceId(e.getProblemId());
            embeddingEntity.setSourceType(SourceType.PROBLEM);
            embeddingEntity.setIsActive(true);
            embeddingEntity.setContent(e.getContent());
            return embeddingEntity;
        }).toList();
        chunkIndexService.insertAll(embeddingEntities,embeddingModel);
        return true;
    }
}
