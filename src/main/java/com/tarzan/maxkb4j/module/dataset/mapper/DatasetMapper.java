package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.core.common.dto.Query;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.DatasetVO;
import org.apache.ibatis.annotations.Param;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
public interface DatasetMapper extends BaseMapper<DatasetEntity>{

    IPage<DatasetVO> selectDatasetPage(Page<DatasetVO> page, @Param("query") Query query, String operate);

}
