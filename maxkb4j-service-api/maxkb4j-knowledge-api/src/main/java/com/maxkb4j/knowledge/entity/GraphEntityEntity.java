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
@TableName(value = "graph_entity", autoResultMap = true)
public class GraphEntityEntity extends BaseEntity {

    private String name;

    private String entityType;

    private String description;

    private String knowledgeId;

    private String documentId;

    private Boolean isActive;

    @TableField(typeHandler = EmbeddingTypeHandler.class)
    private List<Float> embedding;

    private Integer dimension;
}