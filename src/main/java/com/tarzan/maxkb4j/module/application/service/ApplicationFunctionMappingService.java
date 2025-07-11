package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationFunctionMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationFunctionMappingMapper;
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
public class ApplicationFunctionMappingService extends ServiceImpl<ApplicationFunctionMappingMapper, ApplicationFunctionMappingEntity>{

    public List<String> getFunctionIdsByAppId(String appId) {
        List<String> functionIds = new ArrayList<>();
        List<ApplicationFunctionMappingEntity> mappingEntities = this.lambdaQuery()
                .select(ApplicationFunctionMappingEntity::getFunctionId)
                .eq(ApplicationFunctionMappingEntity::getApplicationId, appId).list();
        if (!CollectionUtils.isEmpty(mappingEntities)) {
            functionIds = mappingEntities.stream().map(ApplicationFunctionMappingEntity::getFunctionId).toList();
        }
        return functionIds;
    }

    @Transactional
    public void updateByAppId(String appId, List<String> functionIdList) {
        this.lambdaUpdate().eq(ApplicationFunctionMappingEntity::getApplicationId, appId).remove();
        if (!CollectionUtils.isEmpty(functionIdList)) {
            List<ApplicationFunctionMappingEntity> mappingList = new ArrayList<>();
            for (String functionId :functionIdList) {
                ApplicationFunctionMappingEntity mapping = new ApplicationFunctionMappingEntity();
                mapping.setApplicationId(appId);
                mapping.setFunctionId(functionId);
                mappingList.add(mapping);
            }
            this.saveBatch(mappingList);
        }
    }
}
