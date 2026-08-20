package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.dto.SourceResource;
import com.maxkb4j.system.dto.TargetResource;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.mapper.ResourceMappingMapper;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.service.IResourceMappingInternalService;
import com.maxkb4j.system.strategy.SourceResourceResolver;
import com.maxkb4j.system.vo.ResourceUseVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ResourceMappingServiceImpl extends ServiceImpl<ResourceMappingMapper, ResourceMappingEntity> implements IResourceMappingInternalService {

    private final UserMapper userMapper;
    private final List<SourceResourceResolver> resolvers;
    private Map<String, SourceResourceResolver> resolverMap;

    @PostConstruct
    private void initResolverMap() {
        this.resolverMap = resolvers.stream().collect(Collectors.toMap(SourceResourceResolver::resourceType, Function.identity()));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void relation(String sourceType, String sourceId, List<TargetResource> targets) {
        this.remove(Wrappers.<ResourceMappingEntity>lambdaQuery().eq(ResourceMappingEntity::getSourceType, sourceType).eq(ResourceMappingEntity::getSourceId, sourceId));
        List<ResourceMappingEntity> list = targets.stream().map(target -> {
            ResourceMappingEntity entity = new ResourceMappingEntity();
            entity.setTargetId(target.getTargetId());
            entity.setSourceType(sourceType);
            entity.setSourceId(sourceId);
            entity.setTargetType(target.getTargetType());
            return entity;
        }).toList();
        this.saveBatch(list);
    }

    @Override
    public void deleteBySourceIds(String sourceType, List<String> sourceIds) {
        if (CollectionUtils.isEmpty(sourceIds)) {
            return;
        }
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.<ResourceMappingEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(sourceType), ResourceMappingEntity::getSourceType, sourceType)
                .in(ResourceMappingEntity::getSourceId, sourceIds);
        this.remove(wrapper);
    }


    @Override
    public IPage<ResourceUseVO> selectDependOnPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        return doPage(resourceType, resourceId, current, size, resourceName, userName, sourceType, Direction.SOURCE_TO_TARGET);
    }

    @Override
    public IPage<ResourceUseVO> selectBeDependedOnPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        return doPage(resourceType, resourceId, current, size, resourceName, userName, sourceType, Direction.TARGET_TO_SOURCE);
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
                .collect(Collectors.toMap(SourceResource::getId, Function.identity()));
        return BeanUtil.copyPage(resourcePage, resource -> {
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