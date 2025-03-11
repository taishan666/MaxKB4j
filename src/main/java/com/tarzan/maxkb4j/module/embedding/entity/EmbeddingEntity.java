package com.tarzan.maxkb4j.module.embedding.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.EmbeddingTypeHandler;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
  * @author tarzan
  * @date 2024-12-30 18:08:16
  */
@Data
@TableName("embedding")
@Document(indexName = "embedding")
public class EmbeddingEntity {

	@TableId
	@Id
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
	@TableField(exist = false)
	@Field(type = FieldType.Text, searchAnalyzer = "standard", analyzer = "standard")
	private String content;
	@TableField(exist = false)
	private float score; // 匹配度得分
	//@TableField(typeHandler = TSVectorTypeHandler.class)
	//private TSVector searchVector;
} 
