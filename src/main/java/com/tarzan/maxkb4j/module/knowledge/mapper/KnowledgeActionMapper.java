package com.tarzan.maxkb4j.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeActionEntity;

public interface KnowledgeActionMapper extends BaseMapper<KnowledgeActionEntity> {
    IPage<KnowledgeActionEntity> pageList(Page<KnowledgeActionEntity> actionPage, String username, String state);
}
