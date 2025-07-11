package com.tarzan.maxkb4j.module.resource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2025-01-21 09:35:03
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("image")
public class ImageEntity extends BaseEntity {
	private byte[] image;
	private String imageName;
} 
