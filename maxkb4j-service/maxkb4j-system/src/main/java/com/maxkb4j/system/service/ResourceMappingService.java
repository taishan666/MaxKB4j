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
import com.maxkb4j.system.entity.SourceResource;
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
        if (sourceType != null && sourceType.length > 0) {
            wrapper.in(ResourceMappingEntity::getSourceType, Arrays.asList(sourceType));
        }
        List<ResourceMappingEntity> targets = this.list(wrapper);
        if (CollectionUtils.isEmpty(targets)) {
            return new Page<>(current, size);
        }
        Map<String, List<String>> groupedIds = targets.stream()
                .collect(Collectors.groupingBy(
                        ResourceMappingEntity::getSourceType,
                        Collectors.mapping(ResourceMappingEntity::getSourceId, Collectors.toList())
                ));
        List<SourceResource> filterSources = filterSources(groupedIds, resourceName, userName);
        if (CollectionUtils.isEmpty(filterSources)) {
            return new Page<>(current, size);
        }
        wrapper.in(ResourceMappingEntity::getSourceId, filterSources.stream().map(SourceResource::getId).toList());
        wrapper.orderByDesc(ResourceMappingEntity::getCreateTime);
        resourcePage = this.page(resourcePage, wrapper);
        if (CollectionUtils.isEmpty(resourcePage.getRecords())) {
            return new Page<>(current, size);
        }
        // 批量查询用户昵称（仅查询涉及的用户）
        List<String> allUserIds = filterSources.stream()
                .map(SourceResource::getUserId)
                .distinct()
                .toList();
        Map<String, String> nicknameMap = userMapper.selectList(Wrappers.<UserEntity>lambdaQuery().select(UserEntity::getId, UserEntity::getNickname)
                        .in(UserEntity::getId, allUserIds)).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        Map<String, SourceResource> resourceMaps = filterSources.stream().collect(Collectors.toMap(SourceResource::getId, e -> e));
        return PageUtil.copy(resourcePage, resource -> {
            ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
            SourceResource sourceResource = resourceMaps.get(resource.getSourceId());
            if (sourceResource != null) {
                vo.setName(sourceResource.getName());
                vo.setDesc(sourceResource.getDesc());
                vo.setIcon(sourceResource.getIcon());
                vo.setType(sourceResource.getType());
                vo.setUsername(nicknameMap.get(sourceResource.getUserId()));
            }
            return vo;
        });
    }


    private List<SourceResource> filterSources(Map<String, List<String>> groupedIds, String resourceName, String userName) {
        List<SourceResource> sources = new ArrayList<>();
        if (groupedIds.containsKey(ResourceType.APPLICATION)) {
            List<ApplicationEntity> apps = applicationMapper.selectList(Wrappers.<ApplicationEntity>lambdaQuery()
                    .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon, ApplicationEntity::getType, ApplicationEntity::getUserId)
                    .like(StringUtils.isNotBlank(resourceName), ApplicationEntity::getName, resourceName)
                    .in(StringUtils.isNotBlank(userName), ApplicationEntity::getUserId, getUserIds(userName))
                    .in(ApplicationEntity::getId, groupedIds.get(ResourceType.APPLICATION)));
            sources.addAll(apps.stream().map(e -> new SourceResource(e.getId(), e.getName(), e.getDesc(), e.getIcon(), e.getType(),e.getUserId())).toList());
        }
        if (groupedIds.containsKey(ResourceType.KNOWLEDGE)) {
            List<KnowledgeEntity> knowledgeList = knowledgeMapper.selectList(Wrappers.<KnowledgeEntity>lambdaQuery()
                    .select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc, KnowledgeEntity::getType, KnowledgeEntity::getUserId)
                    .like(StringUtils.isNotBlank(resourceName), KnowledgeEntity::getName, resourceName)
                    .in(StringUtils.isNotBlank(userName), KnowledgeEntity::getUserId, getUserIds(userName))
                    .in(KnowledgeEntity::getId, groupedIds.get(ResourceType.KNOWLEDGE)));
            sources.addAll(knowledgeList.stream().map(e -> new SourceResource(e.getId(), e.getName(), e.getDesc(), "", String.valueOf(e.getType()),e.getUserId())).toList());
        }
        if (groupedIds.containsKey(ResourceType.TOOL)) {
            List<ToolEntity> tools = toolMapper.selectList(Wrappers.<ToolEntity>lambdaQuery()
                    .select(ToolEntity::getId, ToolEntity::getName, ToolEntity::getDesc, ToolEntity::getIcon, ToolEntity::getToolType, ToolEntity::getUserId)
                    .like(StringUtils.isNotBlank(resourceName), ToolEntity::getName, resourceName)
                    .in(StringUtils.isNotBlank(userName), ToolEntity::getUserId, getUserIds(userName))
                    .in(ToolEntity::getId, groupedIds.get(ResourceType.TOOL)));
            sources.addAll(tools.stream().map(e -> new SourceResource(e.getId(), e.getName(), e.getDesc(), e.getIcon(), e.getToolType(),e.getUserId())).toList());
        }
        return sources;
    }

    private List<String> getUserIds(String userName) {
        LambdaQueryWrapper<UserEntity> userWrapper = Wrappers.lambdaQuery();
        userWrapper.select(UserEntity::getId);
        userWrapper.like(UserEntity::getNickname, userName);
        List<String> userIds = userMapper.selectList(userWrapper).stream().map(UserEntity::getId).toList();
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of("-1");
        }
        return userIds;
    }

}
