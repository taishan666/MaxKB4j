package com.maxkb4j.system.vo;

import com.maxkb4j.system.entity.UserResourcePermissionEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceUserPermissionVO extends UserResourcePermissionEntity {

    private String id;
    private String userId;
    private String username;
    private String nickname;
    private String permission;
}
