package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.dataset.domain.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.TextChunkVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
public interface EmbeddingMapper extends BaseMapper<EmbeddingEntity>{

    List<TextChunkVO> embeddingSearch(List<String> datasetIds, List<String> excludeParagraphIds,@Param("maxResults") int maxResults, @Param("minScore") double minScore, @Param("referenceEmbedding") float[]  referenceEmbedding );
 
}
