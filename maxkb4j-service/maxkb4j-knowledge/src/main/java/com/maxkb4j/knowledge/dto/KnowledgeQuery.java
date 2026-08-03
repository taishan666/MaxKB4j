package com.maxkb4j.knowledge.dto;

import com.maxkb4j.core.support.permission.PermissionScopeAware;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeQuery implements PermissionScopeAware {
    private String name;
    private String createUser;
    private String folderId;
    private List<String> targetIds;
    private Boolean isAdmin=false;
    private Integer type;
}
