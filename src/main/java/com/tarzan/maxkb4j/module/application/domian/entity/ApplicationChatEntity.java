package com.tarzan.maxkb4j.module.application.domian.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application_chat",autoResultMap = true)
public class ApplicationChatEntity extends BaseEntity {
    private String summary;
    private String applicationId;
    private String chatUserId;
    private String chatUserType;
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject asker;
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject meta;
    private Integer starNum;
    private Integer trampleNum;
    private Integer chatRecordCount;
    private Integer markSum;
    private Boolean isDeleted;
} 
