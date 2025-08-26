package com.tarzan.maxkb4j.module.system.resourcepermission.vo;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.system.resourcepermission.entity.UserResourcePermissionEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserResourcePermissionVO extends UserResourcePermissionEntity {

    private String name;
    private String icon;
    private String folderId;
    private JSONObject permission;
}
