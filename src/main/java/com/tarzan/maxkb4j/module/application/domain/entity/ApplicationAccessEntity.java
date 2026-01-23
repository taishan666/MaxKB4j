package com.tarzan.maxkb4j.module.application.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import com.tarzan.maxkb4j.common.typehandler.JSONBTypeHandler;
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
