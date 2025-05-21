package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-25 17:18:42
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_function_mapping")
public class ApplicationFunctionMappingEntity extends BaseEntity {

	private String applicationId;
	private String functionId;
} 
