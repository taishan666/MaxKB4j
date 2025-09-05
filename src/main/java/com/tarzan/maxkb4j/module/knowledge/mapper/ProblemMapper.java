package com.tarzan.maxkb4j.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ProblemVO;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
public interface ProblemMapper extends BaseMapper<ProblemEntity>{

    IPage<ProblemVO> pageByDatasetId(Page<ProblemEntity> problemPage, String datasetId,String content);


}
