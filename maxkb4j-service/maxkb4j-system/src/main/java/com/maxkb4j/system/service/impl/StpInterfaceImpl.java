package com.maxkb4j.system.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.constant.Operate;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.entity.UserResourcePermissionEntity;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义权限加载接口实现类
 */
@RequiredArgsConstructor
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final IUserResourcePermissionInternalService userResourcePermissionService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (getActiveUser(loginId) == null) {
            return List.of();
        }
        Set<String> permissions = new LinkedHashSet<>(getWorkspaceDefaultPermission());
        List<UserResourcePermissionEntity> userResourcePermissions = userResourcePermissionService.getByUserId(String.valueOf(loginId));
        for (UserResourcePermissionEntity userResourcePermission : userResourcePermissions) {
            permissions.addAll(getWorkspaceResourcePermission(userResourcePermission));
        }
        return new ArrayList<>(permissions);
    }

    private Set<String> getWorkspaceDefaultPermission(){
        return Arrays.stream(PermissionEnum.values()).filter(e-> Operate.CREATE.equals(e.getOperate())||Operate.READ.equals(e.getOperate())).map(PermissionEnum::getResourcePerm).collect(Collectors.toSet());
    }
    private Set<String> getWorkspaceResourcePermission(UserResourcePermissionEntity userResourcePermission){
        List<PermissionEnum> resourcePermissionEnums = PermissionEnum.getPermissions(
                userResourcePermission.getAuthTargetType(), userResourcePermission.getPermissionList());
        return resourcePermissionEnums.stream().map(e-> {
            String targetId=userResourcePermission.getTargetId();
            return e.getResourcePerm(targetId);
        }).collect(Collectors.toSet());
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserEntity user = getActiveUser(loginId);
        if (user == null || user.getRole() == null) {
            return List.of();
        }
        return List.of(user.getRole());
    }

    private UserEntity getActiveUser(Object loginId) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(UserEntity::getId, loginId);
        wrapper.eq(UserEntity::getIsActive, true);
        wrapper.select(UserEntity::getId, UserEntity::getRole);
        return userMapper.selectOne(wrapper);
    }

}
