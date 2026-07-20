package com.maxkb4j.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
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
