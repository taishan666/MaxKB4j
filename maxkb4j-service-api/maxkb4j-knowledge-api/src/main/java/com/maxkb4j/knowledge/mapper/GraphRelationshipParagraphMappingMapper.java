package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.GraphRelationshipParagraphMappingEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GraphRelationshipParagraphMappingMapper extends BaseMapper<GraphRelationshipParagraphMappingEntity> {

    List<String> getParagraphIdsByRelationshipIds(@Param("relationshipIds") List<String> relationshipIds);

    List<String> getParagraphIdsByRelationshipIdsAndKnowledgeId(@Param("relationshipIds") List<String> relationshipIds,
                                                                 @Param("knowledgeId") String knowledgeId);
}