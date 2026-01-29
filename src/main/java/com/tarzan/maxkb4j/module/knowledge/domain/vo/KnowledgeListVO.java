package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import lombok.Data;

@Data
public class KnowledgeListVO {
    private String id;
    private String name;
    private String desc;
    private Integer type;
    private String folderId;
}
