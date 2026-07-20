package com.maxkb4j.trigger.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskMapper;
import com.maxkb4j.trigger.service.IEventTriggerTaskService;
import org.springframework.stereotype.Service;

@Service
public class EventTriggerTaskServiceImpl extends ServiceImpl<EventTriggerTaskMapper, EventTriggerTaskEntity> implements IEventTriggerTaskService {
}
