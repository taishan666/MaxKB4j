package com.tarzan.maxkb4j.module.dataset.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "dataset")
public class DatasetEntity extends BaseEntity {
    
    private String name;
    
    private String desc;
    
    private String type;
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject meta;
    
    private String userId;
    
    private String embeddingModelId;
} 
