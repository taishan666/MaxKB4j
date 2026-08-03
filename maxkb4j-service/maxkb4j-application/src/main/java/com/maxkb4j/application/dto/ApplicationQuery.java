package com.maxkb4j.application.dto;

import com.maxkb4j.core.support.permission.PermissionScopeAware;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationQuery implements PermissionScopeAware {
    private String name;
    private String publishStatus;
    private String createUser;
    private String folderId;
    private String type;
    private List<String> targetIds;
    private Boolean isAdmin=false;
}