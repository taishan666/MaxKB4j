package com.maxkb4j.trigger.vo;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EventTriggerVO extends EventTriggerDTO {
    private String createUser;
    private String nextRunTime;
    private List<ApplicationEntity> applicationTaskList;
    private List<ToolEntity> toolTaskList;
}
