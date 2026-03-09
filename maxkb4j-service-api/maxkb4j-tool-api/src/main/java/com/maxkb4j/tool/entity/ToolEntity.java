package com.maxkb4j.tool.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.domain.base.entity.BaseEntity;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import com.maxkb4j.common.typehandler.ToolInputParamsTypeHandler;
import com.maxkb4j.tool.dto.ToolInputField;
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

    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONArray initFieldList;

    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject initParams;

    private String userId;
    private Boolean isActive;
    private String toolType;
    private String label;
    private String scope;
    private String icon;
    private String templateId;
    private String folderId;
    private String version;
} 
