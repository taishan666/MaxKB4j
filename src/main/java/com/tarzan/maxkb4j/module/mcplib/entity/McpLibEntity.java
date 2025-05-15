package com.tarzan.maxkb4j.module.mcplib.entity;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2025-04-25 22:00:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mcp_lib")
public class McpLibEntity extends BaseEntity {

    private String name;

    private String desc;

    private String sseUrl;

    @TableField(typeHandler = JOSNBTypeHandler.class)
    private JSONArray mcpTools;

    private String userId;
    private Boolean isActive;
    private String permissionType;
} 
