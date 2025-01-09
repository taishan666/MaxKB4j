package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
public interface ParagraphMapper extends BaseMapper<ParagraphEntity>{

    List<ParagraphVO> retrievalParagraph(List<UUID> paragraphIds);

    void updateStatusById(UUID id, int type, int status,int up,int next);

    void updateStatusByDocIds(List<UUID> docIds, int type, int status,int up,int next);
}
