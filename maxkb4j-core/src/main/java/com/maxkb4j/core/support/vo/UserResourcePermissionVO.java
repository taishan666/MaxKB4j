package com.maxkb4j.core.support.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户资源权限视图对象。
 *
 * <p>作为资源权限分页（{@code IResourcePermissionPageProvider} SPI）的统一返回模型，
 * 由各业务模块（application/knowledge/tool/model）的 provider 填充，
 * system 模块消费。置于 core 以便跨模块共享而不污染任一业务 api 契约。
 */
@Data
public class UserResourcePermissionVO {
    private String id;
    private String name;
    private String icon;
    private String folderId;
    private String permission;
    private String workspaceId;
    private String authTargetType;
    private String targetId;
    private String authType;
    private List<String> permissionList;
    private String userId;

}
