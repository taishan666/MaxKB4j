package com.tarzan.maxkb4j.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.core.common.dto.Query;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import org.apache.ibatis.annotations.Param;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
public interface KnowledgeMapper extends BaseMapper<KnowledgeEntity>{

    IPage<KnowledgeVO> selectDatasetPage(Page<KnowledgeVO> page, @Param("query") Query query, String operate);

}
