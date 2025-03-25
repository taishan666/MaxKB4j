package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.handler.type.JOSNBTypeHandler;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-28 18:47:27
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_work_flow_version")
public class ApplicationWorkFlowVersionEntity extends BaseEntity {
	@TableField(typeHandler = JOSNBTypeHandler.class)
	private JSONObject workFlow;
	private String applicationId;
	private String name;
	private String publishUserId;
	private String publishUserName;

}
