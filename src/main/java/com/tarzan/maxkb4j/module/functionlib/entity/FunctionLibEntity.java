package com.tarzan.maxkb4j.module.functionlib.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
 /**
  * @author tarzan
  * @date 2025-01-25 22:00:45
  */
@Data
@TableName("function_lib")
public class FunctionLibEntity extends BaseEntity {

	private String name;
	
	private String desc;
	
	private String code;
	
	private JSONObject[] inputFieldList;
	
	private String userId;
	
	private Boolean isActive;
	
	private String permissionType;
} 
