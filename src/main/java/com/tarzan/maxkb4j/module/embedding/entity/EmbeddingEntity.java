package com.tarzan.maxkb4j.module.embedding.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.EmbeddingTypeHandler;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.TSVectorTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.dto.TSVector;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
  * @author tarzan
  * @date 2024-12-30 18:08:16
  */
@Data
@TableName("embedding")
public class EmbeddingEntity {
	@TableId
	@TableField(fill = FieldFill.INSERT,typeHandler = UUIDTypeHandler.class)
	private UUID id;
	
	private String sourceId;
	
	private String sourceType;
	
	private Boolean isActive;
	@TableField(typeHandler = EmbeddingTypeHandler.class)
	private List<Float> embedding;
	@TableField(typeHandler = JOSNBTypeHandler.class)
	private JSONObject meta;
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID datasetId;
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID documentId;
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID paragraphId;
	@TableField(typeHandler = TSVectorTypeHandler.class)
	private TSVector searchVector;
} 
