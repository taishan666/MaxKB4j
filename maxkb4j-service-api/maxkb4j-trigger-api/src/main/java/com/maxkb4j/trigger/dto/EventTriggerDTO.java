package com.maxkb4j.trigger.dto;

import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventTriggerDTO extends EventTriggerEntity {
    private List<EventTriggerTaskEntity> triggerTask;
    private List<String> idList;
}
