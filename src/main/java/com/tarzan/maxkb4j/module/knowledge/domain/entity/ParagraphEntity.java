package com.tarzan.maxkb4j.module.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
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
	private String datasetId;
	private String documentId;
/*	@TableField(typeHandler = JOSNBTypeHandler.class,fill = FieldFill.INSERT)
	private JSONObject statusMeta;*/

}
