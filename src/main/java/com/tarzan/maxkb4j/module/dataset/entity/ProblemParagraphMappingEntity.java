package com.tarzan.maxkb4j.module.dataset.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import lombok.Data;
import java.util.Date;
import java.util.UUID;

/**
  * @author tarzan
  * @date 2024-12-27 11:23:44
  */
@Data
@TableName("problem_paragraph_mapping")
public class ProblemParagraphMappingEntity {
	
	@TableField(fill = FieldFill.INSERT)
	private Date createTime;
	
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Date updateTime;
	
	@TableField(fill = FieldFill.INSERT,typeHandler = UUIDTypeHandler.class)
	private UUID id;
	
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID datasetId;
	
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID documentId;
	
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID paragraphId;
	
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID problemId;
} 
