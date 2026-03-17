package com.maxkb4j.trigger.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.common.mp.base.BaseEntity;
import com.maxkb4j.common.typehandler.JSONBTypeHandler;
import com.maxkb4j.tool.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
    @TableField(exist = false)
    private String type;
    @TableField(exist = false)
    private String icon;
    @TableField(exist = false)
    private String name;
}
