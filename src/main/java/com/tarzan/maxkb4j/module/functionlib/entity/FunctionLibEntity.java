package com.tarzan.maxkb4j.module.functionlib.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import com.tarzan.maxkb4j.core.handler.type.FunctionInputParamsTypeHandler;
import com.tarzan.maxkb4j.module.functionlib.dto.FunctionInputField;
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

    @TableField(typeHandler = FunctionInputParamsTypeHandler.class)
    private List<FunctionInputField> inputFieldList;

    private String userId;
    private Boolean isActive;
    private String permissionType;
    private Integer type;
} 
