package com.tarzan.maxkb4j.module.dataset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;

import java.util.UUID;

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
    
	@JsonProperty("user_id")
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID userId;
    
	@JsonProperty("embedding_mode_id")
    @TableField(typeHandler = UUIDTypeHandler.class)
    private UUID embeddingModeId;
} 
