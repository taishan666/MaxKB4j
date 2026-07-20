package com.maxkb4j.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 小峰
 * @date 2026-04-08 17:33:32
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("resource_mapping")
public class ResourceMappingEntity extends BaseEntity {
    private String sourceId;
    private String sourceType;
    private String targetId;
    private String targetType;
}
