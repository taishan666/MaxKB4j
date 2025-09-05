package com.tarzan.maxkb4j.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ProblemParagraphVO;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
public interface ProblemParagraphMapper extends BaseMapper<ProblemParagraphEntity>{

    List<ProblemParagraphVO> getProblems(String datasetId,List<String>  docIds);


    List<ProblemEntity> getProblemsByParagraphId(String paragraphId);
}
