package com.maxkb4j.model.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import com.maxkb4j.common.typehandler.ModelCredentialTypeHandler;
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
