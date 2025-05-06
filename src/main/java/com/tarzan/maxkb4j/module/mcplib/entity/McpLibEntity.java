package com.tarzan.maxkb4j.module.mcplib.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.JOSNBListTypeHandler;
import com.tarzan.maxkb4j.module.functionlib.dto.McpToolParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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

    private String config;

    @TableField(typeHandler = JOSNBListTypeHandler.class)
    private List<McpToolParams> mcpTools;

    private String userId;
    private Boolean isActive;
    private String permissionType;
} 
