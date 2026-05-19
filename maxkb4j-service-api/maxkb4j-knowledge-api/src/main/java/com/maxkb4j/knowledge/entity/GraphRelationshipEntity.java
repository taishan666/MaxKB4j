package com.maxkb4j.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.EmbeddingTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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

    @TableField(typeHandler = EmbeddingTypeHandler.class)
    private List<Float> embedding;

    private Integer dimension;
}