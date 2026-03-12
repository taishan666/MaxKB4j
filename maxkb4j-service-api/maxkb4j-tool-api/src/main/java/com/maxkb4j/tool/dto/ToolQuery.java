package com.maxkb4j.tool.dto;

import lombok.Data;

@Data
public class ToolQuery{
    private String name;
    private String createUser;
    private String folderId;
    private String scope;
    private String toolType;
    private Boolean isActive;
}
