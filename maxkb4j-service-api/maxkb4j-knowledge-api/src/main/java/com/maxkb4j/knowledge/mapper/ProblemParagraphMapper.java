package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.entity.ProblemParagraphEntity;
import com.maxkb4j.knowledge.vo.ProblemParagraphVO;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
public interface ProblemParagraphMapper extends BaseMapper<ProblemParagraphEntity>{

    List<ProblemParagraphVO> getProblems(String knowledgeId, List<String>  docIds);

    List<ProblemEntity> getProblemsByParagraphId(String paragraphId);
}
