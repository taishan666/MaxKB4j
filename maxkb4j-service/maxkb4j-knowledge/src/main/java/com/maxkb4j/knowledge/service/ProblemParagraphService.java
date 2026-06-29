package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.ProblemParagraphVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final IDataStore compositeStore;

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
        compositeStore.deleteByProblemIdAndParagraphId(knowledgeId,problemId,paragraphId);
        return this.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getKnowledgeId, knowledgeId)
                .remove();
    }

    @Transactional
    public void reVector(ProblemEntity problem) {
        List<ProblemParagraphEntity> ppList=this.lambdaQuery().eq(ProblemParagraphEntity::getProblemId, problem.getId()).eq(ProblemParagraphEntity::getKnowledgeId, problem.getKnowledgeId()).list();
       if (CollectionUtils.isNotEmpty(ppList)){
           List<EmbeddingEntity> embeddingEntities=new ArrayList<>();
           for (ProblemParagraphEntity pp : ppList) {
               EmbeddingEntity embeddingEntity = EmbeddingEntity.builder()
                       .knowledgeId(pp.getKnowledgeId())
                       .documentId(pp.getDocumentId())
                       .paragraphId(pp.getParagraphId())
                       .sourceId(pp.getProblemId())
                       .sourceType(SourceType.PROBLEM)
                       .content(problem.getContent())
                       .isActive(true)
                       .build();
               embeddingEntities.add(embeddingEntity);
           }
           compositeStore.deleteProblemByIds(problem.getKnowledgeId(),List.of(problem.getId()));
           EmbeddingModel embeddingModel=knowledgeModelService.getEmbeddingModel(problem.getKnowledgeId());
           compositeStore.upsert(embeddingModel,embeddingEntities);
       }
    }

    public boolean createProblemsIndex(List<ProblemParagraphVO> associations , EmbeddingModel embeddingModel) {
        List<EmbeddingEntity> embeddingEntities=associations.stream().map(e -> EmbeddingEntity.builder()
                .knowledgeId(e.getKnowledgeId())
                .documentId(e.getDocumentId())
                .paragraphId(e.getParagraphId())
                .sourceId(e.getProblemId())
                .sourceType(SourceType.PROBLEM)
                .content(e.getContent())
                .isActive(true)
                .build()).toList();
        compositeStore.upsert(embeddingModel,embeddingEntities);
        return true;
    }
}
