package com.maxkb4j.user.vo;

import com.maxkb4j.system.entity.ResourceMappingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceUseVO extends ResourceMappingEntity {

    private String name;
    private String desc;
    private String icon;
    private String workspaceId;
    private String type;
    private String folderId;
    private String username;
}
