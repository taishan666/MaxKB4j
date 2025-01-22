package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.handler.StringSetTypeHandler;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
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
	@JsonProperty("secret_key")
	private String secretKey;
	@JsonProperty("is_active")
	private Boolean isActive;
	private String applicationId;
	private String userId;
	@JsonProperty("allow_cross_domain")
	private Boolean allowCrossDomain;
	@JsonProperty("cross_domain_list")
	@TableField(typeHandler = StringSetTypeHandler.class)
	private Set<String> crossDomainList;
} 
