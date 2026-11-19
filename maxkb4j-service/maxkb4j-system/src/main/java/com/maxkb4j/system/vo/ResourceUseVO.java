package com.maxkb4j.system.vo;

import lombok.Data;

@Data
public class ResourceUseVO {
    private String id;
    private String name;
    private String desc;
    private String icon;
    private String workspaceId;
    /** 来源对象自身的类型字段值，如 ToolEntity.toolType、KnowledgeEntity.type。 */
    private String type;
    private String folderId;
    private String username;
    /** 来源资源类别：Tool / Knowledge / Model / Application。 */
    private String sourceType;
    private String sourceId;
    private String targetType;
    private String targetId;
}
