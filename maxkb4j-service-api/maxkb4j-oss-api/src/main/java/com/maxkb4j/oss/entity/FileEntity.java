package com.maxkb4j.oss.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
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
