package com.tarzan.maxkb4j.module.mcplib.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2025-04-25 22:00:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("function_lib")
public class McpLibEntity extends BaseEntity {

    private String name;

    private String desc;

    private String code;

    private String userId;
    private Boolean isActive;
    private String permissionType;
} 
