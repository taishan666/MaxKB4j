package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.base.entity.BaseEntity;
import com.tarzan.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
  * @author tarzan
  * @date 2024-12-25 17:00:26
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "document")
public class DocumentEntity extends BaseEntity {
	
	private String name;
	
	private Integer charLength;
	
	private String status;
	
	private Boolean isActive;
	
	private Integer type;
	
	@TableField(typeHandler = JSONBTypeHandler.class)
	private JSONObject meta;
	
	private String knowledgeId;
	
	private String hitHandlingMethod;
	
	private Double directlyReturnSimilarity;
	
	@TableField(typeHandler = JSONBTypeHandler.class,fill = FieldFill.INSERT)
	private JSONObject statusMeta;

} 
