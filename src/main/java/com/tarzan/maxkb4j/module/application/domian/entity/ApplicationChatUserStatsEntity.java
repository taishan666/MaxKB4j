package com.tarzan.maxkb4j.module.application.domian.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
  * @author tarzan
  * @date 2024-12-29 10:34:03
  */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_chat_user_stats")
public class ApplicationChatUserStatsEntity extends BaseEntity {
	
	private Integer accessNum;
	private Integer intraDayAccessNum;
	private String applicationId;
	private String chatUserId;
	private String chatUserType;
} 
