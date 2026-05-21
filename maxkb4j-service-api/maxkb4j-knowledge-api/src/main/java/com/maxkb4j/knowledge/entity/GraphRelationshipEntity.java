package com.maxkb4j.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "graph_relationship", autoResultMap = true)
public class GraphRelationshipEntity extends BaseEntity {

    private String sourceEntityId;

    private String targetEntityId;

    private String description;

    private String keywords;

    private Double weight;

    private String knowledgeId;

    private String documentId;

    private Boolean isActive;

}