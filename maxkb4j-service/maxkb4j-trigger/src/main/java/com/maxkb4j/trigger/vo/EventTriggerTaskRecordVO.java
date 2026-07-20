package com.maxkb4j.trigger.vo;

import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventTriggerTaskRecordVO extends EventTriggerTaskRecordEntity {
    private String sourceName;
}
