package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.entity.UserResourcePermissionEntity;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.mapper.UserResourcePermissionMapper;
import com.maxkb4j.core.support.IResourcePermissionPageProvider;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import com.maxkb4j.core.support.ResourcePermissionQuerySupport;
import com.maxkb4j.system.vo.ResourceUserPermissionVO;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserResourcePermissionServiceImpl extends ServiceImpl<UserResourcePermissionMapper, UserResourcePermissionEntity> implements IUserResourcePermissionInternalService {
    private final String DEFAULT_ID = "default";
    private final UserMapper userMapper;
    private final UserContext userContext;
    /**
     * 按资源类型分派的权限分页查询 SPI 集合，由各业务模块（application/knowledge/tool/model）
     * 在运行期注册实现，避免本模块反向编译依赖业务模块。
     */
    private final List<IResourcePermissionPageProvider> resourcePermissionProviders;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean ownerSave(String targetType, List<String> targetIds, String userId) {
        List<UserResourcePermissionEntity> entityList = targetIds.stream().map(targetId -> {
            UserResourcePermissionEntity entity = new UserResourcePermissionEntity();
            entity.setAuthTargetType(targetType);
            entity.setTargetId(targetId);
            entity.setUserId(userId);
            entity.setPermissionList(List.of(Permission.VIEW, Permission.MANAGE));
            entity.setAuthType("RESOURCE_PERMISSION_GROUP");
            entity.setWorkspaceId(DEFAULT_ID);
            return entity;
        }).toList();
        return this.saveBatch(entityList);
    }

    @Override
    public boolean remove(String targetType, String targetId) {
        return this.lambdaUpdate().eq(UserResourcePermissionEntity::getAuthTargetType, targetType).eq(UserResourcePermissionEntity::getTargetId, targetId).remove();
    }

    @Override
    public boolean remove(String targetType, List<String> targetIds) {
        if (CollectionUtils.isEmpty(targetIds)) {
            return false;
        }
        return this.lambdaUpdate().eq(UserResourcePermissionEntity::getAuthTargetType, targetType).in(UserResourcePermissionEntity::getTargetId, targetIds).remove();
    }

    public boolean update(String type, String targetId, String userId) {
        return this.lambdaUpdate().eq(UserResourcePermissionEntity::getAuthTargetType, type).eq(UserResourcePermissionEntity::getTargetId, targetId).eq(UserResourcePermissionEntity::getUserId, userId).update();
    }


    public IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size, String name, String[] permissions) {
        Map<String, String> permissionMap = resolvePermissionMap(
                Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                        .eq(UserResourcePermissionEntity::getUserId, userId)
                        .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                        .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID),
                UserResourcePermissionEntity::getTargetId);
        Set<String> permissionFilter = toPermissionSet(permissions);
        return resourcePermissionProviders.stream()
                .filter(provider -> provider.supportType().equals(type))
                .findFirst()
                .map(provider -> provider.pageResource(current, size, name, permissionMap, permissionFilter))
                .orElseGet(() -> new Page<>(current, size));
    }

    public IPage<ResourceUserPermissionVO> resourceUserPermissionPage(String resourceId, String type, int current, int size, String nickname, String username, String[] permissions) {
        Map<String, String> permissionMap = resolvePermissionMap(
                Wrappers.<UserResourcePermissionEntity>lambdaQuery()
                        .eq(UserResourcePermissionEntity::getTargetId, resourceId)
                        .eq(UserResourcePermissionEntity::getAuthTargetType, type)
                        .eq(UserResourcePermissionEntity::getWorkspaceId, DEFAULT_ID),
                UserResourcePermissionEntity::getUserId);
        ResourcePermissionQuerySupport.PermissionIdFilter filter = ResourcePermissionQuerySupport.buildPermissionIdFilter(permissionMap, toPermissionSet(permissions));
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


    @Transactional(rollbackFor = Exception.class)
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

    @Transactional(rollbackFor = Exception.class)
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

    @Override
    public List<String> getTargetIds(String authTargetType, String userId, String permission) {
        List<UserResourcePermissionEntity> userResourcePermissions = this.lambdaQuery()
                .select(UserResourcePermissionEntity::getTargetId, UserResourcePermissionEntity::getPermissionList)
                .eq(UserResourcePermissionEntity::getUserId, userId)
                .eq(UserResourcePermissionEntity::getAuthTargetType, authTargetType)
                .list();
        return userResourcePermissions.stream()
                .filter(entity -> CollectionUtils.isNotEmpty(entity.getPermissionList()) && entity.getPermissionList().contains(permission))
                .map(UserResourcePermissionEntity::getTargetId)
                .toList();
    }


    public List<UserResourcePermissionEntity> getByUserId(String userId) {
        return this.lambdaQuery().eq(UserResourcePermissionEntity::getUserId, userId).list();
    }

}
