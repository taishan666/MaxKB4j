package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.common.domain.base.entity.BaseEntity;
import com.tarzan.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
  * @author tarzan
  * @date 2024-12-25 17:00:26
  */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
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

	public DocumentEntity(String knowledgeId, String name, Integer type) {
		super.setId(IdWorker.get32UUID());
		this.knowledgeId = knowledgeId;
		// 限制 name 长度不超过 150 个字符
		if (name != null && name.length() > 150) {
			this.name = name.substring(0, 150);
		} else {
			this.name = name;
		}
		this.type = type;
		this.status = "nn0";
		this.isActive = true;
		this.charLength = 0;
		this.meta = new JSONObject();
		this.statusMeta = new JSONObject();
		this.hitHandlingMethod = "optimization";
		this.directlyReturnSimilarity = 0.9;
	}
}
