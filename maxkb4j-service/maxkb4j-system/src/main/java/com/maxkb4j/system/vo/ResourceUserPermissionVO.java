package com.maxkb4j.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class ResourceUserPermissionVO {

    private String id;
    private String userId;
    private String username;
    private String nickname;
    private String permission;
    private String workspaceId;
    private String authTargetType;
    private String targetId;
    private String authType;
    private List<String> permissionList;
}
