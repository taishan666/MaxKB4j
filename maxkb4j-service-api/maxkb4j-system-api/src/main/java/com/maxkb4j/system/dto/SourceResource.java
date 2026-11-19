package com.maxkb4j.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SourceResource {
    private String id;
    private String name;
    private String desc;
    private String icon;
    /** 资源类别：{@code ResourceType.TOOL} / {@code ResourceType.KNOWLEDGE} 等。 */
    private String sourceType;
    /** 来源对象自身的类型字段值，如 ToolEntity.toolType、KnowledgeEntity.type。 */
    private String type;
    private String userId;
}
