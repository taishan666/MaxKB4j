package com.maxkb4j.tool.dto;

import com.maxkb4j.core.support.permission.PermissionScopeAware;
import lombok.Data;

import java.util.List;

@Data
public class ToolQuery implements PermissionScopeAware {
    private String name;
    private String createUser;
    private String folderId;
    private String scope;
    private String toolType;
    private List<String> toolTypeList;
    private Boolean isActive;
    private List<String> targetIds;
    private Boolean isAdmin=false;
}
