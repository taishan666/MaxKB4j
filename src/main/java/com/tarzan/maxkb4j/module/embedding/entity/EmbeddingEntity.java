package com.tarzan.maxkb4j.module.embedding.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.dto.TSVector;
import com.tarzan.maxkb4j.handler.EmbeddingTypeHandler;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.TSVectorTypeHandler;
import lombok.Data;

import java.util.List;

/**
  * @author tarzan
  * @date 2024-12-30 18:08:16
  */
@Data
@TableName("embedding")
public class EmbeddingEntity {

	@TableId
	private String id;
	
	private String sourceId;
	
	private String sourceType;
	
	private Boolean isActive;
	@TableField(typeHandler = EmbeddingTypeHandler.class)
	private List<Float> embedding;
	@TableField(typeHandler = JOSNBTypeHandler.class)
	private JSONObject meta;
	private String datasetId;
	private String documentId;
	private String paragraphId;
	@TableField(typeHandler = TSVectorTypeHandler.class)
	private TSVector searchVector;
} 
