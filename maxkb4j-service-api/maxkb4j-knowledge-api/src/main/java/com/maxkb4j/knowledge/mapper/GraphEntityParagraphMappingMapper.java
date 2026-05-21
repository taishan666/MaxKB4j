package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.GraphEntityParagraphMappingEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GraphEntityParagraphMappingMapper extends BaseMapper<GraphEntityParagraphMappingEntity> {

    List<String> getParagraphIdsByEntityIdsAndKnowledgeId(@Param("entityIds") List<String> entityIds,
                                                           @Param("knowledgeId") String knowledgeId);
}