package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.entity.TargetResource;

import java.util.List;

public interface IResourceMappingService extends IService<ResourceMappingEntity> {


    boolean relation(String sourceType, String sourceId,String targetType, String targetId);

    boolean relation(String sourceType, String sourceId, List<TargetResource> targets);

    boolean deleteBySourceId(String sourceType,String sourceId);
}
