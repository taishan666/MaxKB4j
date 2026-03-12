package com.maxkb4j.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-27 11:13:27
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("paragraph")
public class ParagraphEntity extends BaseEntity {
	private String title;
	private String content;
	private String status;
	private Integer hitNum;
	private Boolean isActive;
	private String knowledgeId;
	private String documentId;
	private Integer position;
/*	@TableField(typeHandler = JOSNBTypeHandler.class,fill = FieldFill.INSERT)
	private JSONObject statusMeta;*/

}
