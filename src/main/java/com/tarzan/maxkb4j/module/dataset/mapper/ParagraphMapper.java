package com.tarzan.maxkb4j.module.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ParagraphVO;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
public interface ParagraphMapper extends BaseMapper<ParagraphEntity>{

    List<ParagraphVO> retrievalParagraph(List<String> paragraphIds);

    void updateStatusByIds(List<String> paragraphIds, int type, int status,int up,int next);

    void updateStatusById(String paragraphId, int type, int status,int up,int next);

    void updateStatusByDocId(String docId, int type, int status,int up,int next);

    void updateStatusByDocIds(List<String> docIds, int type, int status,int up,int next);
}
