package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.entity.SourceResource;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.system.strategy.SourceResourceResolver;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.mapper.UserMapper;
import com.maxkb4j.user.vo.ResourceUseVO;
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
public class ResourceMappingService extends ServiceImpl<ResourceMappingMapper, ResourceMappingEntity> {

    private final UserMapper userMapper;
    private final Map<String, SourceResourceResolver> resolverMap;

    public ResourceMappingService(UserMapper userMapper, List<SourceResourceResolver> resolvers) {
        this.userMapper = userMapper;
        this.resolverMap = resolvers.stream()
                .collect(Collectors.toMap(SourceResourceResolver::resourceType, Function.identity()));
    }

    public IPage<ResourceUseVO> selectPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        return doPage(resourceType, resourceId, current, size, resourceName, userName, sourceType, Direction.TARGET_TO_SOURCE);
    }

    public IPage<ResourceUseVO> selectMappingResourcePage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        return doPage(resourceType, resourceId, current, size, resourceName, userName, sourceType, Direction.SOURCE_TO_TARGET);
    }

    private IPage<ResourceUseVO> doPage(String resourceType, String resourceId, int current, int size,
                                        String resourceName, String userName, String[] sourceType, Direction direction) {
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(direction.pivotType, resourceType);
        wrapper.eq(direction.pivotId, resourceId);
        if (sourceType != null && sourceType.length > 0) {
            wrapper.in(ResourceMappingEntity::getSourceType, Arrays.asList(sourceType));
        }
        List<ResourceMappingEntity> targets = this.list(wrapper);
        if (CollectionUtils.isEmpty(targets)) {
            return new Page<>(current, size);
        }
        Map<String, List<String>> groupedIds = targets.stream()
                .collect(Collectors.groupingBy(
                        direction.otherTypeGetter,
                        Collectors.mapping(direction.otherIdGetter, Collectors.toList())
                ));
        List<String> userIdFilter = StringUtils.isNotBlank(userName) ? getUserIds(userName) : null;
        List<SourceResource> filterSources = resolveSources(groupedIds, resourceName, userIdFilter);
        if (CollectionUtils.isEmpty(filterSources)) {
            return new Page<>(current, size);
        }
        wrapper.in(direction.otherId, filterSources.stream().map(SourceResource::getId).toList());
        wrapper.orderByDesc(ResourceMappingEntity::getCreateTime);
        Page<ResourceMappingEntity> resourcePage = this.page(new Page<>(current, size), wrapper);
        if (CollectionUtils.isEmpty(resourcePage.getRecords())) {
            return new Page<>(current, size);
        }
        List<String> allUserIds = filterSources.stream()
                .map(SourceResource::getUserId)
                .distinct()
                .toList();
        Map<String, String> nicknameMap = userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .select(UserEntity::getId, UserEntity::getNickname)
                        .in(UserEntity::getId, allUserIds)).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        Map<String, SourceResource> resourceMaps = filterSources.stream()
                .collect(Collectors.toMap(SourceResource::getId, e -> e));
        return PageUtil.copy(resourcePage, resource -> {
            ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
            SourceResource sourceResource = resourceMaps.get(direction.otherIdGetter.apply(resource));
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

    private List<SourceResource> resolveSources(Map<String, List<String>> groupedIds, String resourceName, List<String> userIdFilter) {
        List<SourceResource> sources = new ArrayList<>();
        groupedIds.forEach((type, ids) -> {
            SourceResourceResolver resolver = resolverMap.get(type);
            if (resolver != null) {
                sources.addAll(resolver.resolve(ids, resourceName, userIdFilter));
            }
        });
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

    private enum Direction {
        TARGET_TO_SOURCE(
                ResourceMappingEntity::getTargetType,
                ResourceMappingEntity::getTargetId,
                ResourceMappingEntity::getSourceType,
                ResourceMappingEntity::getSourceId,
                ResourceMappingEntity::getSourceId
        ),
        SOURCE_TO_TARGET(
                ResourceMappingEntity::getSourceType,
                ResourceMappingEntity::getSourceId,
                ResourceMappingEntity::getTargetType,
                ResourceMappingEntity::getTargetId,
                ResourceMappingEntity::getTargetId
        );

        final SFunction<ResourceMappingEntity, ?> pivotType;
        final SFunction<ResourceMappingEntity, ?> pivotId;
        final Function<ResourceMappingEntity, String> otherTypeGetter;
        final Function<ResourceMappingEntity, String> otherIdGetter;
        final SFunction<ResourceMappingEntity, ?> otherId;

        Direction(SFunction<ResourceMappingEntity, ?> pivotType,
                  SFunction<ResourceMappingEntity, ?> pivotId,
                  Function<ResourceMappingEntity, String> otherTypeGetter,
                  Function<ResourceMappingEntity, String> otherIdGetter,
                  SFunction<ResourceMappingEntity, ?> otherId) {
            this.pivotType = pivotType;
            this.pivotId = pivotId;
            this.otherTypeGetter = otherTypeGetter;
            this.otherIdGetter = otherIdGetter;
            this.otherId = otherId;
        }
    }
}
