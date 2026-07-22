package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;

public interface IProblemParagraphService extends IService<ProblemParagraphEntity> {
    boolean association(String knowledgeId, String docId, String paragraphId, String problemId);
    boolean unAssociation(String knowledgeId, String docId, String paragraphId, String problemId);
}
