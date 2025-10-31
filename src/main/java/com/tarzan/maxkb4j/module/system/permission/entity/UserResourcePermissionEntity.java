package com.tarzan.maxkb4j.module.system.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.StringListTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-08-27 14:06:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "user_resource_permission",autoResultMap = true)
public class UserResourcePermissionEntity extends BaseEntity {
    private String workspaceId;
    private String authTargetType;
    private String targetId;
    private String authType;
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> permissionList;
    private String userId;

}
