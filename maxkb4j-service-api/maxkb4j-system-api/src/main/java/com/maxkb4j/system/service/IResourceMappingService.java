package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.system.entity.ResourceMappingEntity;

public interface IResourceMappingService extends IService<ResourceMappingEntity> {


    boolean ownerSave(String resourceName, String sourceType, String sourceId, String targetId, String userId);

    boolean deleteByKnowledgeId(String sourceId);
}
