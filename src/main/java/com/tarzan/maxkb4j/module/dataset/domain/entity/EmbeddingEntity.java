package com.tarzan.maxkb4j.module.dataset.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.handler.type.EmbeddingTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Data
@TableName(value = "embedding",autoResultMap = true)
@Document(collection = "embedding")
public class EmbeddingEntity {

	@TableId
	@Id
	private String id;

	private String sourceId;

	private String sourceType;

	private Boolean isActive;
	@TableField(typeHandler = EmbeddingTypeHandler.class)
	@Transient
	private List<Float> embedding;
	@TableField(typeHandler = JOSNBTypeHandler.class)
	private JSONObject meta;
	private String datasetId;
	private String documentId;
	private String paragraphId;
	@TableField(exist = false)
	@TextIndexed
	private String content;
	@TableField(exist = false)
	@TextScore
	private float score; // 匹配度得分
	//@TableField(typeHandler = TSVectorTypeHandler.class)
	//private TSVector searchVector;
} 
