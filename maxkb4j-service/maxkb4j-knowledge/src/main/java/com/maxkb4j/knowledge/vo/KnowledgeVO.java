package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeVO extends KnowledgeEntity {
    private int charLength;
    private int resourceCount;
    private int documentCount;
    private String nickname;
}

