package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemParagraphVO;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
@Service
public class ProblemParagraphService extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity>{

    public List<ProblemParagraphVO> getProblemsByDocIds(List<UUID>  docIds){
        return baseMapper.getProblemsByDocIds(docIds);
    }

    public List<ProblemParagraphVO> getProblemsByDatasetId(UUID datasetId){
        return baseMapper.getProblemsByDatasetId(datasetId);
    }

    public List<ProblemEntity> getProblemsByParagraphId(UUID paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }
}
