package com.tarzan.maxkb4j.module.dataset.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-27 11:13:27
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("paragraph")
public class ParagraphEntity extends BaseEntity {
	
	private String content;
	
	private String title;
	
	private String status;
	@JsonProperty("hit_num")
	private Integer hitNum;
	@JsonProperty("is_active")
	private Boolean isActive;
	@JsonProperty("dataset_id")
	private String datasetId;
	@JsonProperty("document_id")
	private String documentId;
	@JsonProperty("status_Meta")
	@TableField(typeHandler = JOSNBTypeHandler.class)
	private JSONObject statusMeta;

	public JSONObject defaultStatusMeta() {
		JSONObject statusMeta = new JSONObject();
		statusMeta.put("state_time", new JSONObject());
		return statusMeta;
	}
} 
