package com.tarzan.maxkb4j.module.model.info.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.JSONBTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.ModelCredentialTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("model")
public class ModelEntity extends BaseEntity {

    private String name;
    
    private String modelType;
    
    private String modelName;
    
    private String provider;

    @TableField(typeHandler = ModelCredentialTypeHandler.class)
    private ModelCredential credential;
    
    private String userId;
    
	@TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    
    private String status;
    
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONArray modelParamsForm;
} 
