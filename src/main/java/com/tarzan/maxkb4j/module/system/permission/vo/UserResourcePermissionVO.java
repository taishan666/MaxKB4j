package com.tarzan.maxkb4j.module.system.permission.vo;

import com.tarzan.maxkb4j.module.system.permission.entity.UserResourcePermissionEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserResourcePermissionVO extends UserResourcePermissionEntity {

    private String name;
    private String icon;
    private String folderId;
    private String permission;
}
