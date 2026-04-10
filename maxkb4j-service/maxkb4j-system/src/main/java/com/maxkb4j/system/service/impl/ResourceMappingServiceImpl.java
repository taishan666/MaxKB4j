package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.entity.TargetResource;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.system.service.IResourceMappingService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ResourceMappingServiceImpl extends ServiceImpl<ResourceMappingMapper, ResourceMappingEntity> implements IResourceMappingService {

    @Override
    public boolean relation(String sourceType, String sourceId, String targetType, String targetId) {
        long count = this.count(Wrappers.<ResourceMappingEntity>lambdaQuery()
                .eq(ResourceMappingEntity::getSourceType, sourceType)
                .eq(ResourceMappingEntity::getSourceId, sourceId)
                .eq(ResourceMappingEntity::getTargetType, targetType)
                .eq(ResourceMappingEntity::getTargetId, targetId));
        if (count > 0){
            return true;
        }
        ResourceMappingEntity entity = new ResourceMappingEntity();
        entity.setTargetId(targetId);
        entity.setSourceType(sourceType);
        entity.setSourceId(sourceId);
        entity.setTargetType(targetType);
        return this.saveOrUpdate(entity);
    }

    @Transactional
    @Override
    public boolean relation(String sourceType, String sourceId, List<TargetResource> targets) {
        this.remove(Wrappers.<ResourceMappingEntity>lambdaQuery().eq(ResourceMappingEntity::getSourceType, sourceType).eq(ResourceMappingEntity::getSourceId, sourceId));
        List<ResourceMappingEntity> list = targets.stream().map(target -> {
            ResourceMappingEntity entity = new ResourceMappingEntity();
            entity.setTargetId(target.getTargetId());
            entity.setSourceType(sourceType);
            entity.setSourceId(sourceId);
            entity.setTargetType(target.getTargetType());
            return entity;
        }).toList();
        return this.saveBatch( list);
    }

    @Override
    public boolean deleteBySourceId(String sourceType, String sourceId) {
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.<ResourceMappingEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(sourceType),ResourceMappingEntity::getSourceType, sourceType)
                .eq(StringUtils.isNotBlank(sourceId),ResourceMappingEntity::getSourceId, sourceId);
        return this.remove(wrapper);
    }

}