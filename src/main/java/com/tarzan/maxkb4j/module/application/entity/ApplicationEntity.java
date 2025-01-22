package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:53
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application")
public class ApplicationEntity extends BaseEntity {

    private String name;

    private String desc;

    private String prologue;

    @JsonProperty("dialogue_number")
    private Integer dialogueNumber;

    @JsonProperty("dataset_setting")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject datasetSetting;

    @JsonProperty("model_setting")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject modelSetting;

    @JsonProperty("problem_optimization")
    private Boolean problemOptimization;

    @JsonProperty("model_id")
    private String modelId;

    @JsonProperty("user_id")
    private String userId;

    private String icon;

    private String type;

    @JsonProperty("work_flow")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject workFlow;

    @JsonProperty("model_params_setting")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject modelParamsSetting;

    @JsonProperty("stt_model_id")
    private String sttModelId;

    @JsonProperty("stt_model_enable")
    private Boolean sttModelEnable;

    @JsonProperty("tts_model_id")
    private String ttsModelId;

    @JsonProperty("tts_model_enable")
    private Boolean ttsModelEnable;

    @JsonProperty("tts_type")
    private String ttsType;

    @JsonProperty("problem_optimization_prompt")
    private String problemOptimizationPrompt;

    @JsonProperty("tts_model_params_setting")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject ttsModelParamsSetting;

    @JsonProperty("clean_time")
    private Integer cleanTime;

    @JsonProperty("file_upload_enable")
    private Boolean fileUploadEnable;

    @JsonProperty("file_upload_setting")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject fileUploadSetting;

    @JsonProperty("dataset_id_list")
    @TableField(exist = false)
    private List<String> datasetIdList;
} 
