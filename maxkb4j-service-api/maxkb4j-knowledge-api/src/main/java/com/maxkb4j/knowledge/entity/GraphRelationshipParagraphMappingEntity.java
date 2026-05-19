package com.maxkb4j.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("graph_relationship_paragraph_mapping")
public class GraphRelationshipParagraphMappingEntity extends BaseEntity {

    private String relationshipId;

    private String paragraphId;

    private String knowledgeId;

    private String documentId;
}