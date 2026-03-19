package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;

public interface IEventTriggerTaskService extends IService<EventTriggerTaskEntity> {
    IPage<EventTriggerTaskEntity> pageList(String id, int current, int size, EventTaskQuery query);
}
