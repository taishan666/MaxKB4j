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
@TableName(value = "event_trigger",autoResultMap = true)
public class EventTriggerEntity extends BaseEntity {

    private String workspaceId;
    private String name;
    private String desc;
    private String triggerType;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject triggerSetting;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    private String userId;
    private Boolean isActive;
}
