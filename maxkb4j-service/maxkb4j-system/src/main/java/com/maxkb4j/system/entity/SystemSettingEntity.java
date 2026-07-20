package com.maxkb4j.system.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;

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
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
} 
