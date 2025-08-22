package com.tarzan.maxkb4j.module.application.domian.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.DatasetSettingTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.LlmModelSettingTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
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

    @TableField(typeHandler = DatasetSettingTypeHandler.class,value = "dataset_setting")
    private DatasetSetting knowledgeSetting;

    @TableField(typeHandler = LlmModelSettingTypeHandler.class)
    private LlmModelSetting modelSetting;

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

    private Boolean sttAutoSend;

    private String ttsModelId;

    private Boolean ttsModelEnable;

    private Boolean ttsAutoplay;

    private String ttsType;

    private Boolean isPublish;

    private Date publishTime;

    private String problemOptimizationPrompt;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject ttsModelParamsSetting;

    /*单位天*/
    private Integer cleanTime;

    private Boolean fileUploadEnable;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject fileUploadSetting;

    @TableField(exist = false)
    private List<String> knowledgeIdList;

    @TableField(exist = false)
    private List<String> mcpIdList;

    @TableField(exist = false)
    private List<String> functionIdList;
} 
