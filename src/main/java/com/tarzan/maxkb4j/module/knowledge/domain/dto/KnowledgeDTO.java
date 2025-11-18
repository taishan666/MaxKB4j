package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeDTO extends KnowledgeEntity {
    private String sourceUrl;
    private String selector;
}
