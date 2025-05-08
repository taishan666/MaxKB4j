package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import lombok.Data;

import java.util.Date;

@Data
@TableName("application_platform")
public class ApplicationPlatformEntity {
    @TableId
    private String applicationId;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject status;
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject config;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
