package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.entity.EventTriggerEntity;

public interface IEventTriggerService extends IService<EventTriggerEntity> {
    IPage<EventTriggerEntity> pageList(int current, int size, EventQuery query);
}
