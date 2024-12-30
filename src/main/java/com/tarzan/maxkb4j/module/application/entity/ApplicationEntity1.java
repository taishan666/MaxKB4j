package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.JOSNObjectTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
  * @author tarzan
  * @date 2024-12-25 13:09:53
  */
@Data
@TableName(value = "application",autoResultMap = true)
public class ApplicationEntity1 {
	
	@JsonProperty("create_time")
	@TableField("create_time")
	private Date createTime;
	
	@JsonProperty("update_time")
	@TableField("update_time")
	private Date updateTime;
	
	@TableId
	@TableField(value = "id", typeHandler = UUIDTypeHandler.class)
	private UUID id;
	
	@TableField("name")
	private String name;
	
	@TableField("t.desc")
	private String desc;
	
	@TableField("prologue")
	private String prologue;
	
	@JsonProperty("dialogue_number")
	@TableField("dialogue_number")
	private Integer dialogueNumber;
	
	@JsonProperty("dataset_setting")
	@TableField(typeHandler = JOSNObjectTypeHandler.class)
	private JSONObject datasetSetting;
	
	@JsonProperty("model_setting")
	@TableField(typeHandler = JOSNObjectTypeHandler.class)
	private JSONObject modelSetting;
	
	@JsonProperty("problem_optimization")
	@TableField("problem_optimization")
	private Boolean problemOptimization;
	
	@JsonProperty("model_id")
	@TableField(value = "model_id",typeHandler = UUIDTypeHandler.class)
	private UUID modelId;
	
	@JsonProperty("user_id")
	@TableField(value = "user_id", typeHandler = UUIDTypeHandler.class)
	private UUID userId;
	
	@TableField("icon")
	private String icon;
	
	@TableField("type")
	private String type;
	
	@JsonProperty("work_flow")
	@TableField(typeHandler = JOSNObjectTypeHandler.class)
	private JSONObject workFlow;
	
	@JsonProperty("model_params_setting")
	@TableField(typeHandler = JOSNObjectTypeHandler.class)
	private JSONObject modelParamsSetting;
	
	@JsonProperty("stt_model_id")
	@TableField(value = "stt_model_id", typeHandler = UUIDTypeHandler.class)
	private UUID sttModelId;
	
	@JsonProperty("stt_model_enable")
	@TableField("stt_model_enable")
	private Boolean sttModelEnable;
	
	@JsonProperty("tts_model_id")
	@TableField(value = "tts_model_id", typeHandler = UUIDTypeHandler.class)
	private UUID ttsModelId;
	
	@JsonProperty("tts_model_enable")
	@TableField("tts_model_enable")
	private Boolean ttsModelEnable;
	
	@JsonProperty("tts_type")
	@TableField("tts_type")
	private String ttsType;
	
	@JsonProperty("problem_optimization_prompt")
	@TableField("problem_optimization_prompt")
	private String problemOptimizationPrompt;
	
	@JsonProperty("tts_model_params_setting")
	@TableField(typeHandler = JOSNObjectTypeHandler.class)
	private JSONObject ttsModelParamsSetting;
	
	@JsonProperty("clean_time")
	@TableField("clean_time")
	private Integer cleanTime;
	
	@JsonProperty("file_upload_enable")
	@TableField("file_upload_enable")
	private Boolean fileUploadEnable;
	
	@JsonProperty("file_upload_setting")
	@TableField(typeHandler = JOSNObjectTypeHandler.class)
	private JSONObject fileUploadSetting;
} 
