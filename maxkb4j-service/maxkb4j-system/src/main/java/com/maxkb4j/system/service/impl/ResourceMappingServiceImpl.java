package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.constant.AppConst;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        this.remove(Wrappers.<ResourceMappingEntity>lambdaQuery()
                .eq(ResourceMappingEntity::getSourceType, sourceType)
                .eq(ResourceMappingEntity::getSourceId, sourceId));
        if (CollectionUtils.isEmpty(targets)) {
            return;
        }
        List<ResourceMappingEntity> mappings = targets.stream().map(target -> {
            ResourceMappingEntity entity = new ResourceMappingEntity();
            entity.setSourceType(sourceType);
            entity.setSourceId(sourceId);
            entity.setTargetId(target.getTargetId());
            entity.setTargetType(target.getTargetType());
            return entity;
        }).toList();
        this.saveBatch(mappings);
    }

    @Override
    public boolean existsMapping(String sourceType, String sourceId, String targetType, String targetId) {
        return this.count(Wrappers.<ResourceMappingEntity>lambdaQuery()
                .eq(ResourceMappingEntity::getSourceType, sourceType)
                .eq(ResourceMappingEntity::getSourceId, sourceId)
                .eq(ResourceMappingEntity::getTargetType, targetType)
                .eq(ResourceMappingEntity::getTargetId, targetId)) > 0;
    }

    @Override
    public void deleteBySourceIds(String sourceType, List<String> sourceIds) {
        if (CollectionUtils.isEmpty(sourceIds)) {
            return;
        }
        this.remove(Wrappers.<ResourceMappingEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(sourceType), ResourceMappingEntity::getSourceType, sourceType)
                .in(ResourceMappingEntity::getSourceId, sourceIds));
    }

    @Override
    public IPage<ResourceUseVO> selectDependOnPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        return doPage(resourceType, resourceId, current, size, resourceName, userName, sourceType, Direction.SOURCE_TO_TARGET);
    }

    @Override
    public IPage<ResourceUseVO> selectBeDependedOnPage(String resourceType, String resourceId, int current, int size, String resourceName, String userName, String[] sourceType) {
        return doPage(resourceType, resourceId, current, size, resourceName, userName, sourceType, Direction.TARGET_TO_SOURCE);
    }

    /**
     * 按方向分页查询中心资源关联的对端资源。
     *
     * <p>资源名称/用户昵称分散在各业务表，无法在一条 SQL 内过滤，因此分三步：
     * 先取该资源的映射行（只查对端类型/ID 两列），交由 {@link SourceResourceResolver}
     * 按类型解析出符合名称/用户过滤的对端资源，最后按过滤后的对端 ID 分页。</p>
     */
    private IPage<ResourceUseVO> doPage(String resourceType, String resourceId, int current, int size,
                                        String resourceName, String userName, String[] sourceTypes, Direction direction) {
        LambdaQueryWrapper<ResourceMappingEntity> wrapper = Wrappers.<ResourceMappingEntity>lambdaQuery()
                .select(direction.otherType, direction.otherId)
                .eq(direction.pivotType, resourceType)
                .eq(direction.pivotId, resourceId);
        // 注意：不能用 in(condition, column, Arrays.asList(sourceTypes)) 的条件重载——
        // 实参 Arrays.asList(null) 仍会被求值，sourceTypes 未传时会 NPE
        if (sourceTypes != null && sourceTypes.length > 0) {
            wrapper.in(direction.otherType, Arrays.asList(sourceTypes));
        }
        List<ResourceMappingEntity> mappings = this.list(wrapper);
        if (CollectionUtils.isEmpty(mappings)) {
            return emptyPage(current, size);
        }
        List<String> userIdFilter = StringUtils.isNotBlank(userName) ? selectUserIds(userName) : null;
        if (userIdFilter != null && userIdFilter.isEmpty()) {
            return emptyPage(current, size);
        }
        Map<String, List<String>> groupedIds = mappings.stream()
                .collect(Collectors.groupingBy(direction.otherType,
                        Collectors.mapping(direction.otherId, Collectors.toList())));
        List<SourceResource> filterSources = resolveSources(groupedIds, resourceName, userIdFilter);
        if (CollectionUtils.isEmpty(filterSources)) {
            return emptyPage(current, size);
        }
        Page<ResourceMappingEntity> resourcePage = this.page(new Page<>(current, size),
                Wrappers.<ResourceMappingEntity>lambdaQuery()
                        .eq(direction.pivotType, resourceType)
                        .eq(direction.pivotId, resourceId)
                        .in(direction.otherType, groupedIds.keySet())
                        .in(direction.otherId, filterSources.stream().map(SourceResource::getId).toList())
                        .orderByDesc(ResourceMappingEntity::getCreateTime));
        if (CollectionUtils.isEmpty(resourcePage.getRecords())) {
            return emptyPage(current, size);
        }
        Map<String, SourceResource> sourceMap = filterSources.stream()
                .collect(Collectors.toMap(SourceResource::getId, Function.identity(), (a, b) -> a));
        Map<String, String> nicknameMap = loadNicknames(resourcePage.getRecords().stream()
                .map(resource -> sourceMap.get(direction.otherId.apply(resource)))
                .filter(Objects::nonNull)
                .map(SourceResource::getUserId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList());
        return BeanUtil.copyPage(resourcePage, resource -> {
            ResourceUseVO vo = BeanUtil.copy(resource, ResourceUseVO.class);
            // targetId 取自映射行本身，不依赖 copy 的同名属性隐式拷贝（分页查询一旦收窄 select 列就会丢失）
            vo.setTargetId(resource.getTargetId());
            SourceResource sourceResource = sourceMap.get(direction.otherId.apply(resource));
            if (sourceResource != null) {
                vo.setName(sourceResource.getName());
                vo.setDesc(sourceResource.getDesc());
                vo.setIcon(sourceResource.getIcon());
                vo.setSourceType(sourceResource.getSourceType());
                vo.setType(sourceResource.getType());
                vo.setFolderId(AppConst.DEFAULT_ID);
                vo.setWorkspaceId(AppConst.DEFAULT_ID);
                vo.setSourceId(sourceResource.getId());
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

    /**
     * 按昵称模糊匹配用户 ID；返回空列表表示无匹配用户，调用方据此直接返回空页。
     */
    private List<String> selectUserIds(String userName) {
        return userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .select(UserEntity::getId)
                        .like(UserEntity::getNickname, userName))
                .stream()
                .map(UserEntity::getId)
                .toList();
    }

    /**
     * 查询用户昵称映射；昵称列允许为 null，不能用 {@link Collectors#toMap}（值为 null 会抛 NPE）。
     */
    private Map<String, String> loadNicknames(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> nicknameMap = new HashMap<>(userIds.size());
        userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                        .select(UserEntity::getId, UserEntity::getNickname)
                        .in(UserEntity::getId, userIds))
                .forEach(user -> nicknameMap.put(user.getId(), user.getNickname()));
        return nicknameMap;
    }

    private IPage<ResourceUseVO> emptyPage(int current, int size) {
        return new Page<>(current, size);
    }

    /**
     * 查询方向：pivot 为中心资源（查询入口），other 为要列出的对端资源。
     */
    private enum Direction {
        /** 被依赖视角：中心资源作为 target，列出引用它的 source。 */
        TARGET_TO_SOURCE(
                ResourceMappingEntity::getTargetType,
                ResourceMappingEntity::getTargetId,
                ResourceMappingEntity::getSourceType,
                ResourceMappingEntity::getSourceId
        ),
        /** 依赖视角：中心资源作为 source，列出它引用的 target。 */
        SOURCE_TO_TARGET(
                ResourceMappingEntity::getSourceType,
                ResourceMappingEntity::getSourceId,
                ResourceMappingEntity::getTargetType,
                ResourceMappingEntity::getTargetId
        );

        final SFunction<ResourceMappingEntity, String> pivotType;
        final SFunction<ResourceMappingEntity, String> pivotId;
        final SFunction<ResourceMappingEntity, String> otherType;
        final SFunction<ResourceMappingEntity, String> otherId;

        Direction(SFunction<ResourceMappingEntity, String> pivotType,
                  SFunction<ResourceMappingEntity, String> pivotId,
                  SFunction<ResourceMappingEntity, String> otherType,
                  SFunction<ResourceMappingEntity, String> otherId) {
            this.pivotType = pivotType;
            this.pivotId = pivotId;
            this.otherType = otherType;
            this.otherId = otherId;
        }
    }

}
