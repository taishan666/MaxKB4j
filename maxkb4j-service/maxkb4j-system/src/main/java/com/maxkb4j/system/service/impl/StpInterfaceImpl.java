package com.maxkb4j.system.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.entity.UserResourcePermissionEntity;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 自定义权限加载接口实现类
 */
@RequiredArgsConstructor
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    /**
     * 所有账号默认拥有的权限码集合，静态常量只计算一次
     */
    private static final List<String> DEFAULT_PERMISSIONS = Stream.of(
            PermissionEnum.APPLICATION_READ,
            PermissionEnum.APPLICATION_CREATE,
            PermissionEnum.APPLICATION_IMPORT,
            PermissionEnum.APPLICATION_BATCH_DELETE,
            PermissionEnum.KNOWLEDGE_CREATE,
            PermissionEnum.KNOWLEDGE_READ,
            PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE,
            PermissionEnum.KNOWLEDGE_DOCUMENT_READ,
            PermissionEnum.KNOWLEDGE_PROBLEM_CREATE,
            PermissionEnum.KNOWLEDGE_BATCH_DELETE,
            PermissionEnum.TOOL_CREATE,
            PermissionEnum.TOOL_DEBUG,
            PermissionEnum.TOOL_READ,
            PermissionEnum.TOOL_IMPORT,
            PermissionEnum.TOOL_BATCH_DELETE,
            PermissionEnum.MODEL_CREATE,
            PermissionEnum.MODEL_READ
    ).map(PermissionEnum::getResourcePerm).toList();

    private final UserMapper userMapper;
    private final IUserResourcePermissionInternalService userResourcePermissionService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<UserResourcePermissionEntity> userResourcePermissions = userResourcePermissionService.getByUserId(String.valueOf(loginId));
        Set<String> permissions = new LinkedHashSet<>(DEFAULT_PERMISSIONS);
        for (UserResourcePermissionEntity userResourcePermission : userResourcePermissions) {
            List<PermissionEnum> resourcePermissionEnums = PermissionEnum.getPermissions(
                    userResourcePermission.getAuthTargetType(), userResourcePermission.getPermissionList());
            for (PermissionEnum resourcePermissionEnum : resourcePermissionEnums) {
                permissions.add(resourcePermissionEnum.getResourcePerm(
                        userResourcePermission.getWorkspaceId(), userResourcePermission.getTargetId()));
            }
        }
        return new ArrayList<>(permissions);
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LambdaQueryWrapper<UserEntity>  wrapper= Wrappers.lambdaQuery();
        wrapper.eq(UserEntity::getId,loginId);
        wrapper.select(UserEntity::getRole);
        UserEntity user = userMapper.selectOne(wrapper);
        if (user==null){
            return List.of();
        }
        return List.of(user.getRole());
    }

}
