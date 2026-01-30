package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.domain.base.entity.BaseEntity;
import com.tarzan.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "knowledge", autoResultMap = true)
public class KnowledgeEntity extends BaseEntity {
    
    private String name;
    private String desc;
    private Integer type;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    private String userId;
    private String embeddingModelId;
    private Integer fileSizeLimit;
    private Integer fileCountLimit;
    private String folderId;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject workFlow;
    private Boolean isPublish;
}
