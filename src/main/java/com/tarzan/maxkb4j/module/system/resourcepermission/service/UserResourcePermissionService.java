package com.tarzan.maxkb4j.module.system.resourcepermission.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.resourcepermission.entity.UserResourcePermissionEntity;
import com.tarzan.maxkb4j.module.system.resourcepermission.mapper.UserResourcePermissionMapper;
import com.tarzan.maxkb4j.module.system.resourcepermission.vo.UserResourcePermissionVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserResourcePermissionService extends ServiceImpl<UserResourcePermissionMapper, UserResourcePermissionEntity> {
    public List<UserResourcePermissionVO> getUseTargets(String type, String userId) {
        System.out.println("userId:"+userId);
        return switch (type) {
            case "APPLICATION" -> getUseApplicationTargets(userId);
            case "KNOWLEDGE" -> List.of();
            case "TOOL" -> List.of();
            case "MODEL" -> List.of();
            default -> List.of();
        };
    }

    public boolean save(String type,String targetId, String userId,String workspaceId) {
        UserResourcePermissionEntity entity = new UserResourcePermissionEntity();
        entity.setAuthTargetType(type);
        entity.setTargetId(targetId);
        entity.setUserId(userId);
        entity.setPermissionList(Set.of("VIEW", "MANAGE"));
        entity.setAuthType("RESOURCE_PERMISSION_GROUP");
        entity.setWorkspaceId(workspaceId);
        return this.save(entity);
    }

    public List<UserResourcePermissionVO> getUseApplicationTargets(String userId) {
        List<UserResourcePermissionEntity> list = this.lambdaQuery().eq(UserResourcePermissionEntity::getAuthTargetType, "APPLICATION").list();
        List<UserResourcePermissionVO> apps = BeanUtil.copyList(list, UserResourcePermissionVO.class);
        apps.forEach(app -> {
            app.setName("测试机");
            app.setIcon("");
            Set<String> permissionList = app.getPermissionList();
            JSONObject permission = new JSONObject();
            permission.put("VIEW", permissionList.contains("VIEW"));
            permission.put("MANAGE", permissionList.contains("MANAGE"));
            app.setPermission(permission);
        });
        return apps;
    }
}