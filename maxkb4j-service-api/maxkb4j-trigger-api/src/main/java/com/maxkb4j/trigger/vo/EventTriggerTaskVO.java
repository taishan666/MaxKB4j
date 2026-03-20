package com.maxkb4j.trigger.vo;

import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventTriggerTaskVO extends EventTriggerTaskEntity {
    private String type;
    private String icon;
    private String name;
}
