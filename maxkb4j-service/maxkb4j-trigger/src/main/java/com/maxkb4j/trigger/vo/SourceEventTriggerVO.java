package com.maxkb4j.trigger.vo;

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
    private ApplicationTaskVO applicationTask;
    private ToolTaskVO toolTask;
}
