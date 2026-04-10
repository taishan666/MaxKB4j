package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.system.service.IResourceMappingService;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.entity.UserResourcePermissionEntity;
import com.maxkb4j.user.mapper.UserMapper;
import com.maxkb4j.user.mapper.UserResourcePermissionMapper;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.vo.ResourceUserPermissionVO;
import com.maxkb4j.user.vo.UserResourcePermissionVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ResourceMappingServiceImpl extends ServiceImpl<ResourceMappingMapper, ResourceMappingEntity> implements IResourceMappingService {
    private final String DEFAULT_ID = "default";


    @Override
    public boolean ownerSave(String resourceName, String sourceType, String sourceId, String targetId, String userId) {
        ResourceMappingEntity entity = new ResourceMappingEntity();
        entity.setId(null);
        entity.setResourceName(resourceName);
        entity.setTargetId(targetId);
        entity.setSourceType(sourceType);
        entity.setSourceId(sourceId);
        entity.setTargetType(ResourceType.MODEL);
        entity.setUserId(userId != null ? userId : StpKit.ADMIN.getLoginIdAsString());
        return this.save(entity);
    }

    @Override
    public boolean deleteByKnowledgeId(String sourceId) {
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(sourceId)) {
            wrapper.eq(ResourceMappingEntity::getSourceId, sourceId);
        }
        return this.remove(wrapper);
    }
}