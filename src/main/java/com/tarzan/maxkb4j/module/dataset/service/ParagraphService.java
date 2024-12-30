package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-27 11:13:27
 */
@Service
public class ParagraphService extends ServiceImpl<ParagraphMapper, ParagraphEntity>{

    public List<ParagraphVO> retrievalParagraph(List<UUID> paragraphIds) {
        return baseMapper.retrievalParagraph(paragraphIds);
    }
}
