package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationDatasetMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationDatasetMappingMapper;
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
public class ApplicationDatasetMappingService extends ServiceImpl<ApplicationDatasetMappingMapper, ApplicationDatasetMappingEntity>{

    public List<String> getDatasetIdsByAppId(String appId) {
        List<String> datasetIds = new ArrayList<>();
        List<ApplicationDatasetMappingEntity> mappingEntities = this.lambdaQuery()
                .select(ApplicationDatasetMappingEntity::getDatasetId)
                .eq(ApplicationDatasetMappingEntity::getApplicationId, appId).list();
        if (!CollectionUtils.isEmpty(mappingEntities)) {
            datasetIds = mappingEntities.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList();
        }
        return datasetIds;
    }

    @Transactional
    public void updateByAppId(String appId, List<String> datasetIdList) {
        this.lambdaUpdate().eq(ApplicationDatasetMappingEntity::getApplicationId, appId).remove();
        if (!CollectionUtils.isEmpty(datasetIdList)) {
            List<ApplicationDatasetMappingEntity> mappingList = new ArrayList<>();
            for (String datasetId : datasetIdList) {
                ApplicationDatasetMappingEntity mapping = new ApplicationDatasetMappingEntity();
                mapping.setApplicationId(appId);
                mapping.setDatasetId(datasetId);
                mappingList.add(mapping);
            }
            this.saveBatch(mappingList);
        }
    }
}
