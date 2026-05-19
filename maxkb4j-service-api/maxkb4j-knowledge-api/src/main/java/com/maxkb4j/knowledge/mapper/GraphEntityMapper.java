package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.GraphEntityEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GraphEntityMapper extends BaseMapper<GraphEntityEntity> {

    List<GraphEntityEntity> entityVectorSearch(@Param("knowledgeIds") List<String> knowledgeIds,
                                                @Param("excludeDocumentIds") List<String> excludeDocumentIds,
                                                @Param("maxResults") int maxResults,
                                                @Param("minScore") double minScore,
                                                @Param("referenceEmbedding") float[] referenceEmbedding,
                                                @Param("dimension") int dimension);

    List<GraphEntityEntity> searchByNames(@Param("knowledgeId") String knowledgeId,
                                           @Param("names") List<String> names);

    List<GraphEntityEntity> searchByNameLike(@Param("knowledgeId") String knowledgeId,
                                              @Param("keyword") String keyword);
}