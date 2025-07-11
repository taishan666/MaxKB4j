package com.tarzan.maxkb4j.module.resource.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
 /**
  * @author tarzan
  * @date 2025-01-21 09:34:51
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("file")
public class FileEntity extends BaseEntity {
	
	private String fileName;
	
	private Integer loid;
	
	private JSONObject meta;
} 
