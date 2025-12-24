package com.tarzan.maxkb4j.module.application.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-25 17:18:42
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_knowledge_mapping")
public class ApplicationKnowledgeMappingEntity extends BaseEntity {

	private String applicationId;
	private String knowledgeId;
} 
