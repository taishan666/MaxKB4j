package com.tarzan.maxkb4j.module.dataset.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-26 10:45:40
 */
@Service
public class ProblemService extends ServiceImpl<ProblemMapper, ProblemEntity>{

    public IPage<ProblemVO> getProblemsByDatasetId(Page<ProblemEntity> problemPage, UUID id) {
        return baseMapper.getProblemsByDatasetId(problemPage, id);
    }
}
