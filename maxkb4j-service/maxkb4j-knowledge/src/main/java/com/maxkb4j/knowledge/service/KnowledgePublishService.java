package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.entity.KnowledgeVersionEntity;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库发布与版本管理服务
 * 负责知识库发布、版本管理、执行动作记录等
 *
 * @author tarzan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePublishService {

    private final IKnowledgeInternalService knowledgeService;
    private final IKnowledgeVersionService knowledgeVersionService;
    private final IKnowledgeActionInternalService knowledgeActionService;
    private final IUserService userService;
    private final UserContext userContext;

    /**
     * 发布知识库
     */
    @Transactional
    public Boolean publish(String id) {
        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setId(id);
        knowledge.setIsPublish(true);
        knowledgeService.updateById(knowledge);
        knowledge = knowledgeService.getById(id);
        KnowledgeVersionEntity knowledgeVersion = new KnowledgeVersionEntity();
        knowledgeVersion.setKnowledgeId(id);
        knowledgeVersion.setName(DateTimeUtil.now());
        knowledgeVersion.setWorkFlow(knowledge.getWorkFlow());
        String userId = userContext.getUserId();
        knowledgeVersion.setPublishUserId(userId);
        knowledgeVersion.setPublishUserName(userService.getUsername(userId));
        return knowledgeVersionService.save(knowledgeVersion);
    }

    /**
     * 查询知识库版本列表
     */
    public List<KnowledgeVersionEntity> knowledgeVersion(String id) {
        return knowledgeVersionService.lambdaQuery().eq(KnowledgeVersionEntity::getKnowledgeId, id).list();
    }

    /**
     * 更新知识库版本
     */
    public Boolean knowledgeVersion(String versionId, KnowledgeVersionEntity knowledgeVersion) {
        knowledgeVersion.setId(versionId);
        return knowledgeVersionService.updateById(knowledgeVersion);
    }

    /**
     * 查询执行动作详情
     */
    public KnowledgeActionEntity action(String actionId) {
        return knowledgeActionService.getById(actionId);
    }

    /**
     * 执行动作分页查询
     */
    public IPage<KnowledgeActionEntity> actionPage(String id, int current, int size, String username, String state) {
        Page<KnowledgeActionEntity> actionPage = new Page<>(current, size);
        return knowledgeActionService.pageList(actionPage, username, state);
    }
}
