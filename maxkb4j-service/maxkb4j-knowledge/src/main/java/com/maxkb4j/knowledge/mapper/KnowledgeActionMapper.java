package com.maxkb4j.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;

public interface KnowledgeActionMapper extends BaseMapper<KnowledgeActionEntity> {
    IPage<KnowledgeActionEntity> pageList(Page<KnowledgeActionEntity> actionPage, String username, String state);
}
