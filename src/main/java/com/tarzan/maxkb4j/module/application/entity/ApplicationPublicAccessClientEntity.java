package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-29 10:34:03
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_public_access_client")
public class ApplicationPublicAccessClientEntity extends BaseEntity {
	
	private Integer accessNum;
	private Integer intraDayAccessNum;
	private String applicationId;
} 
