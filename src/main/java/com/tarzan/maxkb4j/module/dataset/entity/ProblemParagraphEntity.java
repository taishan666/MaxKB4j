package com.tarzan.maxkb4j.module.dataset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;

/**
  * @author tarzan
  * @date 2024-12-27 11:23:44
  */
@Data
@TableName("problem_paragraph_mapping")
public class ProblemParagraphEntity extends BaseEntity {

	private String datasetId;
	private String documentId;
	private String paragraphId;
	private String problemId;
} 
