package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.UUID;

/**
  * @author tarzan
  * @date 2024-12-25 17:18:42
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_dataset_mapping")
public class ApplicationDatasetMappingEntity extends BaseEntity {

	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID applicationId;
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID datasetId;
} 
