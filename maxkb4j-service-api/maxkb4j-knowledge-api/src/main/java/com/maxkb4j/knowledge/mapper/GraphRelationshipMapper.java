package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.GraphRelationshipEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GraphRelationshipMapper extends BaseMapper<GraphRelationshipEntity> {

    List<GraphRelationshipEntity> searchByKeywords(@Param("knowledgeId") String knowledgeId,
                                                     @Param("keywords") List<String> keywords);

    List<GraphRelationshipEntity> searchByEntityIds(@Param("knowledgeId") String knowledgeId,
                                                      @Param("entityIds") List<String> entityIds);
}