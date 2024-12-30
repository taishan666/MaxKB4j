package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_chat")
public class ApplicationChatEntity extends BaseEntity {
	@JsonProperty("abstract")
    private String digest;
	@JsonProperty("application_id")
    private String applicationId;
	@JsonProperty("client_id")
    private String clientId;
	@JsonProperty("is_deleted")
    private Boolean isDeleted;
} 
