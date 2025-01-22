package com.tarzan.maxkb4j.module.dataset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;


/**
  * @author tarzan
  * @date 2024-12-25 17:00:26
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "document")
public class DocumentEntity extends BaseEntity {
	
	private String name;
	
	@JsonProperty("char_length")
	private Integer charLength;
	
	private String status;
	
	@JsonProperty("is_active")
	private Boolean isActive;
	
	private String type;
	
	@TableField(typeHandler = JOSNBTypeHandler.class)
	private JSONObject meta;
	
	@JsonProperty("dataset_id")
	private String datasetId;
	
	@JsonProperty("hit_handling_method")
	private String hitHandlingMethod;
	
	@JsonProperty("directly_return_similarity")
	private Double directlyReturnSimilarity;
	
	@TableField(typeHandler = JOSNBTypeHandler.class)
	@JsonProperty("status_meta")
	private JSONObject statusMeta;
} 
