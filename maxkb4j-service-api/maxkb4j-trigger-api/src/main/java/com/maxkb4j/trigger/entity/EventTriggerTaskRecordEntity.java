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
@TableName(value = "event_trigger_task_record", autoResultMap = true)
public class EventTriggerTaskRecordEntity extends BaseEntity {
    private String sourceType;
    private String sourceId;
    private String triggerId;
    private String triggerTaskId;
    private String state;
    @TableField(typeHandler = JSONBTypeHandler.class)
    private JSONObject meta;
    private Float runTime;
    private String taskRecordId;
}
