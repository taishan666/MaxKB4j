package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;

import java.util.List;

public interface IProblemParagraphService extends IService<ProblemParagraphEntity> {

    /**
     * 按问题 ID 列表查询处于激活状态的问题-段落映射，供问题路召回把 problemId 映射为段落。
     * @param problemIds 问题 ID 列表
     * @return 激活的问题-段落映射列表
     */
    List<ProblemParagraphEntity> getActivePPbyProblemIds(List<String> problemIds);

    boolean association(String knowledgeId, String docId, String paragraphId, String problemId);
    boolean unAssociation(String knowledgeId, String docId, String paragraphId, String problemId);

    List<ProblemEntity> getProblemsByParagraphId(String paragraphId);
}