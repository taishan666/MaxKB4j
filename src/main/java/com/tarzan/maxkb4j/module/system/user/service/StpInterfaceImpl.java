package com.tarzan.maxkb4j.module.system.user.service;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.module.system.permission.entity.UserResourcePermissionEntity;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.constants.Operate;
import com.tarzan.maxkb4j.module.system.user.constants.ResourceConst;
import com.tarzan.maxkb4j.module.system.user.constants.ResourceType;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
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
        List<UserResourcePermissionEntity> userResourcePermissions = userResourcePermissionService.getByUserId(userId);
        List<String> permissions = new ArrayList<>();
        permissions.add(ResourceConst.APPLICATION + ":" + Operate.READ + ":/WORKSPACE/default/" + ResourceType.APPLICATION + "/default");
        permissions.add(ResourceConst.APPLICATION + ":" + Operate.CREATE + ":/WORKSPACE/default/" + ResourceType.APPLICATION + "/default");
        permissions.add(ResourceConst.KNOWLEDGE + ":" + Operate.READ + ":/WORKSPACE/default/" + ResourceType.KNOWLEDGE + "/default");
        permissions.add(ResourceConst.KNOWLEDGE + ":" + Operate.CREATE + ":/WORKSPACE/default/" + ResourceType.KNOWLEDGE + "/default");
        permissions.add(ResourceConst.KNOWLEDGE_DOCUMENT + ":" + Operate.READ + ":/WORKSPACE/default/" + ResourceType.KNOWLEDGE + "/default");
      //  permissions.add(ResourceConst.KNOWLEDGE_DOCUMENT + ":" + Operate.CREATE + ":/WORKSPACE/default/" + ResourceType.KNOWLEDGE + "/default");
        permissions.add(ResourceConst.TOOL + ":" + Operate.READ + ":/WORKSPACE/default/" + ResourceType.TOOL + "/default");
        permissions.add(ResourceConst.TOOL + ":" + Operate.CREATE + ":/WORKSPACE/default/" + ResourceType.TOOL + "/default");
        permissions.add(ResourceConst.MODEL + ":" + Operate.READ + ":/WORKSPACE/default/" + ResourceType.MODEL + "/default");
        permissions.add(ResourceConst.MODEL + ":" + Operate.CREATE + ":/WORKSPACE/default/" + ResourceType.MODEL + "/default");
        for (UserResourcePermissionEntity permission : userResourcePermissions) {
            List<PermissionEnum> resourcePermissionEnums = PermissionEnum.getPermissions(permission.getAuthTargetType(),permission.getPermissionList());
            resourcePermissionEnums.forEach(e -> {
                String perm = e.getResource() + ":" + e.getOperate() + ":/WORKSPACE/" + permission.getWorkspaceId() + "/" + e.getResourceType() + "/" + permission.getTargetId();
                permissions.add(perm);
            });

        }
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
