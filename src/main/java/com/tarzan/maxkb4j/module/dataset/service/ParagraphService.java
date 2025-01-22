package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
@Service
public class ParagraphService extends ServiceImpl<ParagraphMapper, ParagraphEntity>{

    public List<ParagraphVO> retrievalParagraph(List<String> paragraphIds) {
        return baseMapper.retrievalParagraph(paragraphIds);
    }

    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusById(id,type,status,type-1,type+1);
    }

    public void updateStatusByDocIds(List<String> docIds, int type,int status)  {
        baseMapper.updateStatusByDocIds(docIds,type,status,type-1,type+1);
    }
}
