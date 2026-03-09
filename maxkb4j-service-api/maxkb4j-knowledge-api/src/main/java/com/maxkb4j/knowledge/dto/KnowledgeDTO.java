package com.maxkb4j.knowledge.dto;

import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeDTO extends KnowledgeEntity {
    private String sourceUrl;
    private String selector;
}
