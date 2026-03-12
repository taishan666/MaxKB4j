package com.maxkb4j.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application_access", autoResultMap = true)
public class ApplicationAccessEntity extends BaseEntity {
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject status;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject config;
}
