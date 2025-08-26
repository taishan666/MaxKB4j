package com.tarzan.maxkb4j.module.system.resourcepermission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * @author tarzan
 * @date 2025-08-27 14:06:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workspace_user_resource_permission")
public class UserResourcePermissionEntity extends BaseEntity {
    private String workspaceId;
    private String authTargetType;
    private String targetId;
    private String authType;
    private Set<String> permissionList;
    private String userId;
}
