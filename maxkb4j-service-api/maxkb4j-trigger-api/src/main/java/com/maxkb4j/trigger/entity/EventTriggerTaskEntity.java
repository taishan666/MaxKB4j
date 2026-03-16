package com.maxkb4j.trigger.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "event_trigger_task",autoResultMap = true)
public class EventTriggerTaskEntity extends BaseEntity {

    private String sourceType;
    private String sourceId;
    private String triggerId;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject parameter;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    private Boolean isActive;
}
