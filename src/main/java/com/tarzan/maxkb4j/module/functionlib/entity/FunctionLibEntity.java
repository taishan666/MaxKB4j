package com.tarzan.maxkb4j.module.functionlib.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import com.tarzan.maxkb4j.handler.type.JOSNBArrayTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("function_lib")
public class FunctionLibEntity extends BaseEntity {

    private String name;

    private String desc;

    private String code;

    @TableField(typeHandler = JOSNBArrayTypeHandler.class)
    private List<JSONObject> inputFieldList;

    private String userId;
    private Boolean isActive;
    private String permissionType;
} 
