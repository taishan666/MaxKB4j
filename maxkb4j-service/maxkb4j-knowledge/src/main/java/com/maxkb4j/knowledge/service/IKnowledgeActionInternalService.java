package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;

/**
 * 知识库动作服务「对内」接口：在 {@link IKnowledgeActionService}（对外跨模块契约）基础上，
 * 补充 MyBatis-Plus {@link IService} 能力与 impl 专用分页查询。
 */
public interface IKnowledgeActionInternalService extends IKnowledgeActionService, IService<KnowledgeActionEntity> {

    IPage<KnowledgeActionEntity> pageList(Page<KnowledgeActionEntity> actionPage, String username, String state);
}
