package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.vo.ProblemVO;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
public interface ProblemMapper extends BaseMapper<ProblemEntity>{

    IPage<ProblemVO> pageByDatasetId(Page<ProblemEntity> problemPage, String knowledgeId, String content);


}
