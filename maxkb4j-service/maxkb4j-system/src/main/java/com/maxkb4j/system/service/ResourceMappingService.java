package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.mapper.UserMapper;
import com.maxkb4j.user.vo.ResourceUseVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 小峰
 * @date 2026-04-08 17:33:32
 */
@Service
@RequiredArgsConstructor
public class ResourceMappingService extends ServiceImpl<ResourceMappingMapper, ResourceMappingEntity> {
    private final UserMapper userMapper;
    private final ApplicationMapper applicationMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final ToolMapper toolMapper;



    public IPage<ResourceUseVO> selectUserPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        Page<ResourceMappingEntity> resourcePage = new Page<>(current, size);
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ResourceMappingEntity::getTargetType, resourceType);
        wrapper.eq(ResourceMappingEntity::getTargetId, resourceId);
        if (StringUtils.isNotBlank(resourceName)) {
            wrapper.like(ResourceMappingEntity::getResourceName, resourceName);
        }
        if (sourceType != null && sourceType.length > 0) {
            wrapper.in(ResourceMappingEntity::getSourceType, Arrays.asList(sourceType));
        }

        // 提前获取用户ID列表，用于后续优化
        List<String> userIds;
        if (StringUtils.isNotBlank(userName)) {
            LambdaQueryWrapper<UserEntity> userWrapper = Wrappers.lambdaQuery();
            userWrapper.select(UserEntity::getId);
            userWrapper.like(UserEntity::getNickname, userName);
            userIds = userMapper.selectList(userWrapper).stream().map(UserEntity::getId).toList();
            if (CollectionUtils.isEmpty(userIds)) {
                return new Page<>(current, size);
            }
            wrapper.in(ResourceMappingEntity::getUserId, userIds);
        }
        wrapper.orderByDesc(ResourceMappingEntity::getCreateTime);

        resourcePage = this.page(resourcePage, wrapper);
        if (CollectionUtils.isEmpty(resourcePage.getRecords())) {
            return new Page<>(current, size);
        }

        // 批量查询用户昵称（仅查询涉及的用户）
        List<String> allUserIds = resourcePage.getRecords().stream()
                .map(ResourceMappingEntity::getUserId)
                .distinct()
                .toList();
        Map<String, String> nicknameMap = userMapper.selectByIds(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        switch (resourceType) {
            case ResourceType.APPLICATION -> {
                Map<String, ApplicationEntity> appMap = batchQueryResources(
                        resourcePage.getRecords(), ResourceType.APPLICATION,
                        ApplicationEntity::getId, applicationMapper::selectByIds);
                return PageUtil.copy(resourcePage, resource -> {
                    ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
                    String sourceId = resource.getSourceId();
                    ApplicationEntity app = appMap.get(sourceId);
                    if (app != null) {
                        vo.setName(app.getName());
                        vo.setDesc(app.getDesc());
                    }
                    vo.setUsername(nicknameMap.get(resource.getUserId()));
                    return vo;
                });
            }
            case ResourceType.KNOWLEDGE -> {
                Map<String, KnowledgeEntity> knowledgeMap = batchQueryResources(
                        resourcePage.getRecords(), ResourceType.KNOWLEDGE,
                        KnowledgeEntity::getId, knowledgeMapper::selectByIds);
                return PageUtil.copy(resourcePage, resource -> {
                    ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
                    String sourceId = resource.getSourceId();
                    KnowledgeEntity knowledge = knowledgeMap.get(sourceId);
                    if (knowledge != null) {
                        vo.setName(knowledge.getName());
                        vo.setDesc(knowledge.getDesc());
                    }
                    vo.setUsername(nicknameMap.get(resource.getUserId()));
                    return vo;
                });
            }
            case ResourceType.TOOL -> {
                Map<String, ToolEntity> toolMap = batchQueryResources(
                        resourcePage.getRecords(), ResourceType.TOOL,
                        ToolEntity::getId, toolMapper::selectByIds);
                return PageUtil.copy(resourcePage, resource -> {
                    ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
                    String sourceId = resource.getSourceId();
                    ToolEntity tool = toolMap.get(sourceId);
                    if (tool != null) {
                        vo.setName(tool.getName());
                        vo.setDesc(tool.getDesc());
                    }
                    vo.setUsername(nicknameMap.get(resource.getUserId()));
                    return vo;
                });
            }
        }
        return PageUtil.copy(resourcePage, resource -> {
            ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
            vo.setUsername(nicknameMap.get(resource.getUserId()));
            return vo;
        });
    }

    /**
     * 批量查询资源，解决 N+1 问题
     */
    private <T> Map<String, T> batchQueryResources(
            List<ResourceMappingEntity> resources,
            String sourceType,
            Function<T, String> idExtractor,
            Function<Collection<String>, List<T>> batchQuery) {
        List<String> ids = resources.stream()
                .filter(r -> sourceType.equals(r.getSourceType()))
                .map(ResourceMappingEntity::getSourceId)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of();
        }
        return batchQuery.apply(ids).stream()
                .collect(Collectors.toMap(idExtractor, t -> t, (a, b) -> a));
    }
}
