package com.tarzan.maxkb4j.module.model.entity;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.JOSNTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("model")
public class ModelEntity extends BaseEntity {

    private String name;
    
	@JsonProperty("model_type")
    private String modelType;
    
	@JsonProperty("model_name")
    private String modelName;
    
    private String provider;
    
    private String credential;
    
	@JsonProperty("user_id")
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID userId;
    
	@TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject meta;
    
    private String status;
    
	@JsonProperty("permission_type")
    private String permissionType;
    
	@JsonProperty("model_params_form")
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONArray modelParamsForm;
} 
