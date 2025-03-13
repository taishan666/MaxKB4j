package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private Integer dialogueNumber;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject datasetSetting;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject modelSetting;

    private Boolean problemOptimization;

    private String modelId;

    private String userId;

    private String icon;

    private String type;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject workFlow;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject modelParamsSetting;

    private String sttModelId;

    private Boolean sttModelEnable;

    private String ttsModelId;

    private Boolean ttsModelEnable;

    private String ttsType;

    private String problemOptimizationPrompt;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject ttsModelParamsSetting;

    private Integer cleanTime;

    private Boolean fileUploadEnable;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject fileUploadSetting;

    @TableField(exist = false)
    private List<String> datasetIdList;
} 
