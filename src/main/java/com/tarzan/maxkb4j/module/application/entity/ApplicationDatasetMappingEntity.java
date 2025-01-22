package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-25 17:18:42
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_dataset_mapping")
public class ApplicationDatasetMappingEntity extends BaseEntity {

	private String applicationId;
	private String datasetId;
} 
