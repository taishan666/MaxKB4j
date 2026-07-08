package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.system.constant.AuthTargetType;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserResourcePermissionServiceImpl extends ServiceImpl<UserResourcePermissionMapper, UserResourcePermissionEntity> implements IUserResourcePermissionService {
    private final String DEFAULT_ID = "default";
    private final ApplicationMapper applicationMapper;
    private final KnowledgeMapper datasetMapper;
    private final ToolMapper toolMapper;
    private final ModelMapper modelMapper;
    private final UserMapper userMapper;
    private final UserContext userContext;

    public boolean ownerSave(String type, String targetId, String userId) {
        UserResourcePermissionEntity entity = new UserResourcePermissionEntity();
        entity.setAuthTargetType(type);
        entity.setTargetId(targetId);
        entity.setUserId(userId);
        entity.setPermissionList(List.of(Permission.VIEW, Permission.MANAGE));
        entity.setAuthType("RESOURCE_PERMISSION_GROUP");
        entity.setWorkspaceId(DEFAULT_ID);
        return this.save(entity);
    }


    public boolean remove(String type, String targetId) {
        return this.lambdaUpdate().eq(UserResourcePermissionEntity::getAuthTargetType, type).eq(UserResourcePermissionEntity::getTargetId, targetId).eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID).remove();
    }

    public boolean update(String type, String targetId, String userId) {
        return this.lambdaUpdate().eq(UserResourcePermissionEntity::getAuthTargetType, type).eq(UserResourcePermissionEntity::getTargetId, targetId).eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID).update();
    }


    public IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size, String name, String[] permissions) {
        Map<String, String> permissionMap = resolvePermissionMap(
                Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                        .eq(UserResourcePermissionEntity::getUserId, userId)
                        .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                        .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID),
                UserResourcePermissionEntity::getTargetId);
        Set<String> permissionFilter = toPermissionSet(permissions);
        return switch (type) {
            case AuthTargetType.APPLICATION -> pageResource(new Page<>(current, size), applicationMapper, ApplicationEntity::getName, ApplicationEntity::getId, name, ApplicationEntity::getIcon, permissionMap, permissionFilter, type, null);
            case AuthTargetType.KNOWLEDGE -> pageResource(new Page<>(current, size), datasetMapper, KnowledgeEntity::getName, KnowledgeEntity::getId, name, e -> "", permissionMap, permissionFilter, type, null);
            case AuthTargetType.TOOL -> pageResource(new Page<>(current, size), toolMapper, ToolEntity::getName, ToolEntity::getId, name, ToolEntity::getIcon, permissionMap, permissionFilter, type, w -> w.eq(ToolEntity::getScope, "WORKSPACE"));
            case AuthTargetType.MODEL -> pageResource(new Page<>(current, size), modelMapper, ModelEntity::getName, ModelEntity::getId, name, ModelEntity::getProvider, permissionMap, permissionFilter, type, null);
            default -> new Page<>(current, size);
        };
    }

    public IPage<ResourceUserPermissionVO> resourceUserPermissionPage(String resourceId, String type, int current, int size, String nickname, String username, String[] permissions) {
        Map<String, String> permissionMap = resolvePermissionMap(
                Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                        .eq(UserResourcePermissionEntity::getTargetId, resourceId)
                        .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                        .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID),
                UserResourcePermissionEntity::getUserId);
        PermissionIdFilter filter = buildPermissionIdFilter(permissionMap, toPermissionSet(permissions));
        if (filter.emptyResult()) {
            return new Page<>(current, size, 0);
        }
        String currentUserId = userContext.getUserId();
        LambdaQueryWrapper<UserEntity> userWrapper = Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getIsActive, true)
                .ne(UserEntity::getId, currentUserId)
                .like(StringUtils.isNotBlank(nickname), UserEntity::getNickname, nickname)
                .like(StringUtils.isNotBlank(username), UserEntity::getUsername, username)
                .orderByAsc(UserEntity::getCreateTime);
        if (CollectionUtils.isNotEmpty(filter.includeIds())) {
            userWrapper.in(UserEntity::getId, filter.includeIds());
        }
        if (CollectionUtils.isNotEmpty(filter.excludeIds())) {
            userWrapper.notIn(UserEntity::getId, filter.excludeIds());
        }
        Page<UserEntity> userPage = userMapper.selectPage(new Page<>(current, size), userWrapper);
        return PageUtil.copy(userPage, e -> {
            ResourceUserPermissionVO vo = new ResourceUserPermissionVO();
            vo.setId(e.getId());
            vo.setNickname(e.getNickname());
            vo.setUsername(e.getUsername());
            vo.setPermission(permissionMap.getOrDefault(e.getId(), Permission.NOT_AUTH));
            vo.setWorkspaceId(DEFAULT_ID);
            vo.setAuthTargetType(type);
            return vo;
        });
    }


    /**
     * 分页查询某类资源，并按名称和权限过滤。
     *
     * @param iconGetter 资源图标的取值方式，不同资源类型图标来源不同（应用/工具取 icon，模型取 provider，知识库为空串）
     */
    private <E extends BaseEntity> IPage<UserResourcePermissionVO> pageResource(
            Page<E> page,
            BaseMapper<E> mapper,
            SFunction<E, String> nameField,
            SFunction<E, String> idField,
            String name,
            Function<E, String> iconGetter,
            Map<String, String> permissionMap,
            Set<String> permissionFilter,
            String type,
            Consumer<LambdaQueryWrapper<E>> extraConditions) {
        PermissionIdFilter filter = buildPermissionIdFilter(permissionMap, permissionFilter);
        if (filter.emptyResult()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }
        LambdaQueryWrapper<E> wrapper = Wrappers.<E>lambdaQuery()
                .like(StringUtils.isNotBlank(name), nameField, name);
        if (CollectionUtils.isNotEmpty(filter.includeIds())) {
            wrapper.in(idField, filter.includeIds());
        }
        if (CollectionUtils.isNotEmpty(filter.excludeIds())) {
            wrapper.notIn(idField, filter.excludeIds());
        }
        if (extraConditions != null) {
            extraConditions.accept(wrapper);
        }
        mapper.selectPage(page, wrapper);
        return PageUtil.copy(page, e -> buildResourceVO(e.getId(), nameField.apply(e), iconGetter.apply(e), type, permissionMap.getOrDefault(e.getId(), Permission.NOT_AUTH)));
    }

    private UserResourcePermissionVO buildResourceVO(String id, String name, String icon, String type, String permission) {
        UserResourcePermissionVO vo = new UserResourcePermissionVO();
        vo.setId(id);
        vo.setName(name);
        vo.setIcon(icon);
        vo.setFolderId(id);
        vo.setWorkspaceId(DEFAULT_ID);
        vo.setAuthTargetType(type);
        vo.setPermission(permission);
        return vo;
    }

    /**
     * 加载权限记录并解析为 {@code id -> 已解析权限} 的映射，key 取自 keyGetter（目标资源 ID 或用户 ID）。
     * 同一 key 存在多条记录时保留首条。
     */
    private Map<String, String> resolvePermissionMap(LambdaQueryWrapper<UserResourcePermissionEntity> wrapper, SFunction<UserResourcePermissionEntity, String> keyGetter) {
        return baseMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(keyGetter, e -> getPermissionFromList(e.getPermissionList()), (a, b) -> a));
    }

    private Set<String> toPermissionSet(String[] permissions) {
        return permissions == null ? Set.of() : Arrays.stream(permissions).collect(Collectors.toSet());
    }

    /**
     * 根据权限过滤条件计算资源/用户 ID 的过滤方式。
     * <p>permissionMap 的 key 为已存在权限记录的 ID，value 为该记录解析后的权限（MANAGE/VIEW/NOT_AUTH）；
     * 不在 map 中的 ID 视为 NOT_AUTH（未授权）。
     * <ul>
     *   <li>过滤为空或同时包含正权限与 NOT_AUTH：不限制，返回全部；</li>
     *   <li>仅正权限：包含命中 ID，命中为空时返回空结果；</li>
     *   <li>仅 NOT_AUTH：排除所有已授权（VIEW/MANAGE）的 ID。</li>
     * </ul>
     */
    private PermissionIdFilter buildPermissionIdFilter(Map<String, String> permissionMap, Set<String> filter) {
        if (filter.isEmpty()) {
            return PermissionIdFilter.none();
        }
        boolean includePositive = filter.contains(Permission.VIEW) || filter.contains(Permission.MANAGE);
        boolean includeNotAuth = filter.contains(Permission.NOT_AUTH);
        if (!includePositive && !includeNotAuth) {
            return PermissionIdFilter.empty();
        }
        if (includePositive && includeNotAuth) {
            return PermissionIdFilter.none();
        }
        if (includePositive) {
            Set<String> matchIds = permissionMap.entrySet().stream()
                    .filter(e -> filter.contains(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            return matchIds.isEmpty() ? PermissionIdFilter.empty() : new PermissionIdFilter(false, matchIds, Set.of());
        }
        // 仅 NOT_AUTH：排除所有已授权 ID（含权限列表为空但存在记录的项）
        Set<String> permittedIds = permissionMap.entrySet().stream()
                .filter(e -> Permission.VIEW.equals(e.getValue()) || Permission.MANAGE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return new PermissionIdFilter(false, Set.of(), permittedIds);
    }

    private String getPermissionFromList(List<String> permissionList) {
        if (CollectionUtils.isNotEmpty(permissionList)) {
            if (permissionList.contains(Permission.MANAGE)) {
                return Permission.MANAGE;
            } else if (permissionList.contains(Permission.VIEW)) {
                return Permission.VIEW;
            } else {
                return Permission.NOT_AUTH;
            }
        } else {
            return Permission.NOT_AUTH;
        }
    }


    @Transactional
    public boolean userPermissionUpdate(String userId, String type, List<UserResourcePermissionVO> list) {
        List<String> targetIds = list.stream().map(UserResourcePermissionVO::getTargetId).toList();
        this.remove(Wrappers.<UserResourcePermissionEntity>lambdaUpdate().eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getAuthTargetType, type).in(UserResourcePermissionEntity::getTargetId, targetIds));
        List<UserResourcePermissionEntity> saveList = list.stream().map(vo -> {
            vo.setPermissionList(getPermissionFromList(vo.getPermission()));
            vo.setUserId(userId);
            vo.setAuthType("RESOURCE_PERMISSION_GROUP");
            vo.setWorkspaceId(DEFAULT_ID);
            vo.setAuthTargetType(type);
            vo.setFolderId(DEFAULT_ID);
            return BeanUtil.copy(vo, UserResourcePermissionEntity.class);
        }).toList();
        return this.saveBatch(saveList);
    }

    @Transactional
    public boolean resourcePermissionUpdate(String resourceId, String type, List<ResourceUserPermissionVO> list) {
        List<String> userIds = list.stream().map(ResourceUserPermissionVO::getUserId).toList();
        this.remove(Wrappers.<UserResourcePermissionEntity>lambdaUpdate().eq(UserResourcePermissionEntity::getTargetId, resourceId).eq(UserResourcePermissionEntity::getAuthTargetType, type).in(UserResourcePermissionEntity::getUserId, userIds));
        List<UserResourcePermissionEntity> saveList = list.stream().map(vo -> {
            vo.setPermissionList(getPermissionFromList(vo.getPermission()));
            vo.setTargetId(resourceId);
            vo.setAuthType("RESOURCE_PERMISSION_GROUP");
            vo.setWorkspaceId(DEFAULT_ID);
            vo.setAuthTargetType(type);
            return BeanUtil.copy(vo, UserResourcePermissionEntity.class);
        }).toList();
        return this.saveBatch(saveList);
    }

    private List<String> getPermissionFromList(String permission) {
        if (Permission.MANAGE.equals(permission)) {
            return List.of(Permission.MANAGE, Permission.VIEW);
        } else if (Permission.VIEW.equals(permission)) {
            return List.of(Permission.VIEW);
        }
        return List.of();
    }

    public List<String> getTargetIds(String authTargetType, String userId) {
        List<UserResourcePermissionEntity> userResourcePermissions = this.lambdaQuery().select(UserResourcePermissionEntity::getTargetId, UserResourcePermissionEntity::getPermissionList).eq(UserResourcePermissionEntity::getUserId, userId).eq(UserResourcePermissionEntity::getAuthTargetType, authTargetType).list();
        return userResourcePermissions.stream().filter(permission -> permission.getPermissionList().contains(Permission.VIEW)).map(UserResourcePermissionEntity::getTargetId).toList();
    }


    public List<UserResourcePermissionEntity> getByUserId(String userId) {
        return this.lambdaQuery().eq(UserResourcePermissionEntity::getUserId, userId).list();
    }

    /**
     * 权限过滤结果：emptyResult 表示无可命中项需返回空页；includeIds/excludeIds 用于在分页查询中按 ID 收窄范围。
     */
    private record PermissionIdFilter(boolean emptyResult, Set<String> includeIds, Set<String> excludeIds) {
        static PermissionIdFilter none() {
            return new PermissionIdFilter(false, Set.of(), Set.of());
        }

        static PermissionIdFilter empty() {
            return new PermissionIdFilter(true, Set.of(), Set.of());
        }
    }

}
