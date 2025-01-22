package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemParagraphVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-27 11:23:44
 */
@Service
public class ProblemParagraphService extends ServiceImpl<ProblemParagraphMapper, ProblemParagraphEntity>{

    public List<ProblemParagraphVO> getProblemsByDatasetId(String datasetId){
        return baseMapper.getProblems(datasetId,null);
    }

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        return baseMapper.getProblemsByParagraphId(paragraphId);
    }
}
