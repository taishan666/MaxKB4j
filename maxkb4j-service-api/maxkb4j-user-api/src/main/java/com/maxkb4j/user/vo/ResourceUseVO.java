package com.maxkb4j.user.vo;

import lombok.Data;

@Data
public class ResourceUseVO {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private String workspaceId;
    private String type;
    private String folderId;
    private String username;
    private String sourceType;
    private String targetType;
}
