package com.maxkb4j.knowledge.vo;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeListVO {
    private String id;
    private String name;
    private String desc;
    private Integer type;
    private String folderId;
    private int charLength;
    private int documentCount;
    private String embeddingModelId;
    private String nickname;
    private Date createTime;
    private String resourceType="knowledge";
}
