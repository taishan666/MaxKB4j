package com.tarzan.maxkb4j.module.application.domian.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.DatasetSettingTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.JSONBTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.LlmModelSettingTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-28 18:47:27
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application_version",autoResultMap = true)
public class ApplicationVersionEntity extends BaseEntity {

	private String applicationId;
	private String applicationName;
	private String publishUserId;
	private String publishUserName;
/*	private String workspaceId;*/
	private String name;

	private String desc;

	private String prologue;

	private Integer dialogueNumber;

	@TableField(typeHandler = DatasetSettingTypeHandler.class)
	private KnowledgeSetting knowledgeSetting;

	@TableField(typeHandler = LlmModelSettingTypeHandler.class)
	private LlmModelSetting modelSetting;

	private Boolean problemOptimization;

	private String modelId;

	private String userId;

	private String icon;

	private String type;

	@TableField(typeHandler = JSONBTypeHandler.class)
	private JSONObject workFlow;

	@TableField(typeHandler = JSONBTypeHandler.class)
	private JSONObject modelParamsSetting;

	private String sttModelId;

	private Boolean sttModelEnable;

	private Boolean sttAutoSend;

	private String ttsModelId;

	private Boolean ttsModelEnable;

	private Boolean ttsAutoplay;

	private String ttsType;

	private String problemOptimizationPrompt;

	@TableField(typeHandler = JSONBTypeHandler.class)
	private JSONObject ttsModelParamsSetting;

	/*单位天*/
	private Integer cleanTime;

	private Boolean fileUploadEnable;

	@TableField(typeHandler = JSONBTypeHandler.class)
	private JSONObject fileUploadSetting;

}
