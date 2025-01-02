package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemParagraphVO;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
public interface ProblemParagraphMapper extends BaseMapper<ProblemParagraphEntity>{
    List<ProblemParagraphVO> getProblemsByDocIds(List<UUID>  docIds);

    List<ProblemParagraphVO> getProblemsByDatasetId(UUID datasetId);
}
