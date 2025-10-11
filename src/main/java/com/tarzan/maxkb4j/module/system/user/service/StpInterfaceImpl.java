package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.system.permission.entity.UserResourcePermissionEntity;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.enums.ResourcePermissionEnum;
import com.tarzan.maxkb4j.module.system.user.mapper.UserMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限加载接口实现类
 */
@AllArgsConstructor
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final UserResourcePermissionService userResourcePermissionService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String userId = loginId.toString();
        List<UserResourcePermissionEntity> userResourcePermissions = userResourcePermissionService.getUserId(userId);
        List<String> permissions = new ArrayList<>();
        for (UserResourcePermissionEntity permission : userResourcePermissions) {
            List<ResourcePermissionEnum> resourcePermissionEnums = ResourcePermissionEnum.getPermissions(permission.getAuthTargetType()).stream().filter(e -> permission.getPermissionList().contains(e.getPermission())).toList();
            resourcePermissionEnums.forEach(e -> {
                String operate = e.getResource() + ":" + e.getOperate() + ":/WORKSPACE/" + permission.getWorkspaceId() + "/" + permission.getAuthTargetType() + "/" + permission.getTargetId();
                permissions.add(operate);
            });
        }
        // permissions.add("x-pack");
        return permissions;
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
        return new ArrayList<>(user.getRole());
    }

}
