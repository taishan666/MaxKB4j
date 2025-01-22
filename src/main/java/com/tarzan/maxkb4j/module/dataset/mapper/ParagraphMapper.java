package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
public interface ParagraphMapper extends BaseMapper<ParagraphEntity>{

    List<ParagraphVO> retrievalParagraph(List<String> paragraphIds);

    void updateStatusById(String id, int type, int status,int up,int next);

    void updateStatusByDocIds(List<String> docIds, int type, int status,int up,int next);
}
