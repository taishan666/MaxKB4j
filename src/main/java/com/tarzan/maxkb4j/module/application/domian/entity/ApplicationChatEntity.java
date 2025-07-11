package com.tarzan.maxkb4j.module.application.domian.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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
    private String overview;
    private String applicationId;
    private String clientId;
    private Boolean isDeleted;
} 
