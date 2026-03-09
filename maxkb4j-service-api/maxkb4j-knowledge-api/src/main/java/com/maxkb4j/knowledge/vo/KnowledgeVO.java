package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.KnowledgeEntity;
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
