package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class EventTriggerTaskService extends ServiceImpl<EventTriggerTaskMapper, EventTriggerTaskEntity> implements IEventTriggerTaskService {

}
