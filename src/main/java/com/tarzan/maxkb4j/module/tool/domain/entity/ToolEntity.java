package com.tarzan.maxkb4j.module.tool.domain.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.ToolInputParamsTypeHandler;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "tool",autoResultMap = true)
public class ToolEntity extends BaseEntity {

    private String name;

    private String desc;

    private String code;

    @TableField(typeHandler = ToolInputParamsTypeHandler.class)
    private List<ToolInputField> inputFieldList;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONArray initFieldList;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONObject initParams;

    private String userId;
    private Boolean isActive;
    private String toolType;
    private String label;
    private String scope;
    private String icon;
} 
