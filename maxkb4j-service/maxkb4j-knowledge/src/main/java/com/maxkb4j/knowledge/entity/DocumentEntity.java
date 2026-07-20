package com.maxkb4j.knowledge.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import com.maxkb4j.knowledge.consts.HitHandlingMethod;
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

	/** 名称最大字符数 */
	public static final int MAX_NAME_LENGTH = 150;

	/** 默认直接返回相似度阈值 */
	public static final double DEFAULT_DIRECTLY_RETURN_SIMILARITY = 0.9;
	/** 文档初始状态码 */
	public static final String STATUS_INITIAL = "nn0";

	public DocumentEntity(String knowledgeId, String name, Integer type) {
		super.setId(IdWorker.get32UUID());
		this.knowledgeId = knowledgeId;
		// 限制 name 长度不超过 MAX_NAME_LENGTH 个字符
		this.name = (name != null && name.length() > MAX_NAME_LENGTH) ? name.substring(0, MAX_NAME_LENGTH) : name;
		this.type = type;
		this.status = STATUS_INITIAL;
		this.isActive = true;
		this.charLength = 0;
		this.meta = new JSONObject();
		this.statusMeta = new JSONObject();
		this.hitHandlingMethod = HitHandlingMethod.HIT_HANDLING_OPTIMIZATION;
		this.directlyReturnSimilarity = DEFAULT_DIRECTLY_RETURN_SIMILARITY;
	}
}
