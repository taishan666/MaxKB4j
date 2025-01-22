package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
public interface ProblemMapper extends BaseMapper<ProblemEntity>{

    IPage<ProblemVO> getProblemsByDatasetId(Page<ProblemEntity> problemPage, String datasetId,String content);


}
