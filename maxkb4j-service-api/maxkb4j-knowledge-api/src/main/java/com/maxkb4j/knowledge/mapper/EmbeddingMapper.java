package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
public interface EmbeddingMapper extends BaseMapper<EmbeddingEntity> {

    /**
     * 嵌入搜索 knowledgeIds 必须是同一个向量模型
     * @param knowledgeIds
     * @param excludeParagraphIds
     * @param maxResults
     * @param minScore
     * @param referenceEmbedding
     * @return
     */
    List<TextChunkVO> embeddingSearch(List<String> knowledgeIds,List<String> excludeDocumentIds, List<String> excludeParagraphIds, @Param("maxResults") int maxResults, @Param("minScore") double minScore, @Param("referenceEmbedding") float[]  referenceEmbedding, int dimension);


}
