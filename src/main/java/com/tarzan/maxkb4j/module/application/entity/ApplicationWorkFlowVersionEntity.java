package com.tarzan.maxkb4j.module.application.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.handler.JOSNBTypeHandler;
import com.tarzan.maxkb4j.handler.UUIDTypeHandler;
import com.tarzan.maxkb4j.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

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
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID applicationId;
	
	private String name;
	@TableField(typeHandler = UUIDTypeHandler.class)
	private UUID publishUserId;
	
	private String publishUserName;
} 
