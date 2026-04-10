package com.maxkb4j.user.vo;

import lombok.Data;

@Data
public class ResourceUseVO{

    private String id;
    private String createTime;
    private String updateTime;
    private String sourceType;
    private String targetType;
    private String sourceId;
    private String targetId;
    private String name;
    private String desc;
    private String icon;
    private String userId;
    private String workspaceId;
    private String type;
    private String folderId;
    private String username;
}
