package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.knowledge.dto.KnowledgeQuery;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.vo.KnowledgeVO;
import org.apache.ibatis.annotations.Param;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
public interface KnowledgeMapper extends BaseMapper<KnowledgeEntity>{

    IPage<KnowledgeVO> selectKnowledgePage(Page<KnowledgeVO> page, @Param("query") KnowledgeQuery query);
}
