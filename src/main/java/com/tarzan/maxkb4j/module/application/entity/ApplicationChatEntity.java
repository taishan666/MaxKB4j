package com.tarzan.maxkb4j.module.application.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("application_chat")
public class ApplicationChatEntity extends BaseEntity {
	@JsonProperty("abstract")
    @TableField(value = "abstract")
    private String digest;
    private String applicationId;
    private String clientId;
    private Boolean isDeleted;
} 
