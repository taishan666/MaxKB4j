package com.maxkb4j.knowledge.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeActionMapper;
import com.maxkb4j.knowledge.service.IKnowledgeActionInternalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class KnowledgeActionServiceImpl extends ServiceImpl<KnowledgeActionMapper, KnowledgeActionEntity> implements IKnowledgeActionInternalService {

    public void updateState(String id, JSONObject details, String state) {
        KnowledgeActionEntity knowledgeActionEntity = baseMapper.selectById(id);
        // 工作流异步完成回调期间记录可能已被级联删除（如删除知识库时按 knowledgeId 清理），
        // 为 null 时跳过更新，避免 NPE 被丢弃的 future 静默吞掉
        if (knowledgeActionEntity == null) {
            log.warn("知识库动作记录已不存在，跳过状态更新: id={}, state={}", id, state);
            return;
        }
        knowledgeActionEntity.setDetails(details);
        knowledgeActionEntity.setState(state);
        Date createTime = knowledgeActionEntity.getCreateTime();
        knowledgeActionEntity.setRunTime((System.currentTimeMillis() - createTime.getTime()) / 1000f);
        baseMapper.updateById(knowledgeActionEntity);
    }


    public IPage<KnowledgeActionEntity> pageList(Page<KnowledgeActionEntity> actionPage, String knowledgeId, String username, String state) {
        return baseMapper.pageList(actionPage, knowledgeId, username, state);
    }
}
