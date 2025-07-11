package com.tarzan.maxkb4j.module.system.setting.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import lombok.Data;
import com.alibaba.fastjson.JSONObject;

import java.util.Date;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@Data
@TableName("system_setting")
public class SystemSettingEntity {
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    //0 邮箱 1 密匙
    @TableId(value = "type", type = IdType.INPUT)
    private Integer type;
    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject meta;
} 
