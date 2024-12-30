package com.tarzan.maxkb4j.module.embedding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
public interface EmbeddingMapper extends BaseMapper<EmbeddingEntity>{

    List<HitTestVO> embeddingSearch(@Param("datasetId") UUID datasetId, @Param("query") HitTestDTO query, @Param("embedding") float[] embedding);

    List<HitTestVO> keywordsSearch(UUID datasetId,@Param("query") HitTestDTO dto);

    List<HitTestVO> HybridSearch(@Param("datasetId")UUID datasetId, @Param("query") HitTestDTO dto,@Param("embedding") float[] embedding);
 
}
