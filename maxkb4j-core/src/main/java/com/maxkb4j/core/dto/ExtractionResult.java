package com.maxkb4j.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtractionResult {

    private String type;
    private String name;
    private String entityType;
    private String description;
    private String sourceEntity;
    private String targetEntity;
    private String keywords;

    public boolean isEntity() {
        return "entity".equals(type);
    }

    public boolean isRelationship() {
        return "relationship".equals(type);
    }
}