package com.tarzan.maxkb4j.module.application.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tarzan.maxkb4j.common.domain.base.entity.BaseEntity;
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
	/*访问次数*/
	private Integer accessNum;
	/*1天内访问次数*/
	private Integer intraDayAccessNum;
	private String applicationId;
	private String chatUserId;
	private String chatUserType;
} 
