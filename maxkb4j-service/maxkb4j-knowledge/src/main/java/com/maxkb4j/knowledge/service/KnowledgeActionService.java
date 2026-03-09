package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeActionMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class KnowledgeActionService extends ServiceImpl<KnowledgeActionMapper, KnowledgeActionEntity> implements IKnowledgeActionService{

    public void updateState(String id, JSONObject details, String state){
        KnowledgeActionEntity knowledgeActionEntity = baseMapper.selectById(id);
        knowledgeActionEntity.setDetails(details);
        knowledgeActionEntity.setState(state);
        Date createTime = knowledgeActionEntity.getCreateTime();
        knowledgeActionEntity.setRunTime((System.currentTimeMillis() - createTime.getTime()) / 1000f);
        baseMapper.updateById(knowledgeActionEntity);
    }

/*    public void updateState(Workflow workflow, ActionStatus actionStatus) {
        if (workflow instanceof KnowledgeWorkflow knowledgeWorkflow) {
            String actionId = knowledgeWorkflow.getKnowledgeParams().getActionId();
            updateState(actionId, knowledgeWorkflow.getRuntimeDetails(), actionStatus.name());
        }
    }*/

    public IPage<KnowledgeActionEntity> pageList(Page<KnowledgeActionEntity> actionPage, String username, String state) {
        return baseMapper.pageList(actionPage, username, state);
    }
}
