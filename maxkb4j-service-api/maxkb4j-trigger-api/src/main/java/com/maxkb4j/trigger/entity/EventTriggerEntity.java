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
@TableName(value = "event_trigger", autoResultMap = true)
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
    @TableField(exist = false)
    private List<EventTriggerTaskEntity> triggerTask;
    @TableField(exist = false)
    private List<String> idList;
    @TableField(exist = false)
    private String createUser;
    @TableField(exist = false)
    private String nextRunTime;
    @TableField(exist = false)
    private String triggerTaskStr;
    @TableField(exist = false)
    private List<ApplicationEntity> applicationTaskList;
    @TableField(exist = false)
    private List<ToolEntity> toolTaskList;

}
