package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.domain.base.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-27 11:23:44
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("problem_paragraph_mapping")
public class ProblemParagraphEntity extends BaseEntity {

	private String knowledgeId;
	private String documentId;
	private String paragraphId;
	private String problemId;
} 
