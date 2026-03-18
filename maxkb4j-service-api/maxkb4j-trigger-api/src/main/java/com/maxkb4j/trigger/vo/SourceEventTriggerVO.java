package com.maxkb4j.trigger.vo;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SourceEventTriggerVO extends EventTriggerEntity {
    private EventTriggerTaskEntity triggerTask;
    private String createUser;
    private String nextRunTime;
    private String triggerTaskStr;
    private ApplicationEntity applicationTask;
    private ToolEntity toolTask;
}
