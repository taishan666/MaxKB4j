package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationMcpMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMcpMappingMapper;
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
public class ApplicationMcpMappingService extends ServiceImpl<ApplicationMcpMappingMapper, ApplicationMcpMappingEntity>{

    public List<String> getMcpIdsByAppId(String appId) {
        List<String> mcpIds = new ArrayList<>();
        List<ApplicationMcpMappingEntity> mcpMappingEntities = this.lambdaQuery()
                .select(ApplicationMcpMappingEntity::getMcpId)
                .eq(ApplicationMcpMappingEntity::getApplicationId, appId).list();
        if (!CollectionUtils.isEmpty(mcpMappingEntities)) {
            mcpIds = mcpMappingEntities.stream().map(ApplicationMcpMappingEntity::getMcpId).toList();
        }
        return mcpIds;
    }

    @Transactional
    public void updateByAppId(String appId, List<String> mcpIdList) {
        this.lambdaUpdate().eq(ApplicationMcpMappingEntity::getApplicationId, appId).remove();
        if (!CollectionUtils.isEmpty(mcpIdList)) {
            List<ApplicationMcpMappingEntity> mappingList = new ArrayList<>();
            for (String mcpId : mcpIdList) {
                ApplicationMcpMappingEntity mapping = new ApplicationMcpMappingEntity();
                mapping.setApplicationId(appId);
                mapping.setMcpId(mcpId);
                mappingList.add(mapping);
            }
            this.saveBatch(mappingList);
        }
    }
}
