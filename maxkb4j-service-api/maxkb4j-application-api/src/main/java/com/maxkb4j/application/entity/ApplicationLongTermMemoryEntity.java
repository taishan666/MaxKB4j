package com.maxkb4j.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tarzan
 * @date 2026-05-11 13:09:53
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "application_long_term_memory", autoResultMap = true)
public class ApplicationLongTermMemoryEntity extends BaseEntity {
    private String memory;
    private String applicationId;
    private String chatUserId;
}
