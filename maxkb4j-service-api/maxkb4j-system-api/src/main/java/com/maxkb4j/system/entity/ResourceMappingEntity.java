package com.maxkb4j.system.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;

import java.util.Date;

/**
 * @author 小峰
 * @date 2026-04-08 17:33:32
 */
@Data
@TableName("resource_mapping")
public class ResourceMappingEntity extends BaseEntity {
    private String sourceId;
    private String sourceType;
    private String targetId;
    private String targetType;
    private String userId;
    private String resourceName;
} 
