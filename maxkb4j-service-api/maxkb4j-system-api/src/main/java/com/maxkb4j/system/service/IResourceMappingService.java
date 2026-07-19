package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.entity.TargetResource;

import java.util.List;

public interface IResourceMappingService extends IService<ResourceMappingEntity> {

    boolean relation(String sourceType, String sourceId, List<TargetResource> targets);

    boolean deleteBySourceId(String sourceType,String sourceId);

    /**
     * 批量删除同一来源类型下多个 sourceId 的资源映射，用 {@code IN (...)} 合并删除。
     */
    boolean deleteBySourceIds(String sourceType, List<String> sourceIds);

}
