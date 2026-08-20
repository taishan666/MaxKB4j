package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.ProblemParagraphVO;
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
public class ProblemParagraphServiceImpl extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity> implements IProblemParagraphService {

    private final ProblemMapper problemMapper;
   // private final KnowledgeModelService knowledgeModelService;
    private final IDataStore compositeStore;

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }

    public List<ProblemParagraphEntity> getActivePPbyProblemIds(List<String> problemIds) {
        return baseMapper.getActivePPbyProblemIds(problemIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean association(String knowledgeId, String docId, String paragraphId, String problemId) {
        ProblemParagraphVO problemParagraph = new ProblemParagraphVO();
        problemParagraph.setKnowledgeId(knowledgeId);
        problemParagraph.setProblemId(problemId);
        problemParagraph.setParagraphId(paragraphId);
        problemParagraph.setDocumentId(docId);
        LambdaQueryWrapper<ProblemEntity> wrapper= Wrappers.<ProblemEntity>lambdaQuery().select(ProblemEntity::getContent).eq(ProblemEntity::getId,problemId);
        ProblemEntity  problem= problemMapper.selectOne(wrapper);
        problemParagraph.setContent(problem.getContent());
        return this.save(problemParagraph);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unAssociation(String knowledgeId, String docId, String paragraphId, String problemId) {
        return this.lambdaUpdate()
                .eq(ProblemParagraphEntity::getParagraphId, paragraphId)
                .eq(ProblemParagraphEntity::getProblemId, problemId)
                .eq(ProblemParagraphEntity::getDocumentId, docId)
                .eq(ProblemParagraphEntity::getKnowledgeId, knowledgeId)
                .remove();
    }

}
