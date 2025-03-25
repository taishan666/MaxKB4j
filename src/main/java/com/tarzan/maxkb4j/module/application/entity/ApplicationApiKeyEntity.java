package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.handler.type.StringSetTypeHandler;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
  * @author tarzan
  * @date 2025-01-02 09:01:12
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_api_key")
public class ApplicationApiKeyEntity extends BaseEntity {
	private String secretKey;
	private Boolean isActive;
	private String applicationId;
	private String userId;
	private Boolean allowCrossDomain;
	@TableField(typeHandler = StringSetTypeHandler.class)
	private Set<String> crossDomainList;
} 
