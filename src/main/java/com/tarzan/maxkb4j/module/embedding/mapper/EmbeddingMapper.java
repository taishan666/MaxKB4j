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

    List<HitTestVO> embeddingSearch(List<UUID> datasetIds, @Param("query") HitTestDTO query, @Param("embedding") float[] embedding);

    List<HitTestVO> keywordsSearch(List<UUID>  datasetIds,@Param("query") HitTestDTO query);

    List<HitTestVO> HybridSearch(List<UUID>  datasetIds, @Param("query") HitTestDTO query,@Param("embedding") float[] embedding);
 
}
