package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeVO extends KnowledgeEntity {
    private int charLength;
    private int applicationMappingCount;
    private int documentCount;
    private List<String> applicationIdList;
    private String nickname;
}
