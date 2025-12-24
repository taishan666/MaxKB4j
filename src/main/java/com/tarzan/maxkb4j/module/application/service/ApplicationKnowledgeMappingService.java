package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationKnowledgeMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationKnowledgeMappingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 17:18:42
 */
@Service
public class ApplicationKnowledgeMappingService extends ServiceImpl<ApplicationKnowledgeMappingMapper, ApplicationKnowledgeMappingEntity>{

    public List<String> getKnowledgeIdsByAppId(String appId) {
        List<String> knowledgeIds = new ArrayList<>();
        List<ApplicationKnowledgeMappingEntity> mappingEntities = this.lambdaQuery()
                .select(ApplicationKnowledgeMappingEntity::getKnowledgeId)
                .eq(ApplicationKnowledgeMappingEntity::getApplicationId, appId).list();
        if (!CollectionUtils.isEmpty(mappingEntities)) {
            knowledgeIds = mappingEntities.stream().map(ApplicationKnowledgeMappingEntity::getKnowledgeId).toList();
        }
        return knowledgeIds;
    }

    @Transactional
    public void updateByAppId(String appId, List<String> knowledgeIdList) {
        this.lambdaUpdate().eq(ApplicationKnowledgeMappingEntity::getApplicationId, appId).remove();
        if (!CollectionUtils.isEmpty(knowledgeIdList)) {
            List<ApplicationKnowledgeMappingEntity> mappingList = new ArrayList<>();
            for (String knowledgeId : knowledgeIdList) {
                ApplicationKnowledgeMappingEntity mapping = new ApplicationKnowledgeMappingEntity();
                mapping.setApplicationId(appId);
                mapping.setKnowledgeId(knowledgeId);
                mappingList.add(mapping);
            }
            this.saveBatch(mappingList);
        }
    }
}
