package com.maxkb4j.trigger.dto;

import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.vo.EventTriggerTaskVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventTriggerDTO extends EventTriggerEntity {
    private List<EventTriggerTaskVO> triggerTask;
    private List<String> idList;
}
