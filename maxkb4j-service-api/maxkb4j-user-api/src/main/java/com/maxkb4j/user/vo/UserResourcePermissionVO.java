package com.maxkb4j.user.vo;

import com.maxkb4j.user.entity.UserResourcePermissionEntity;
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
