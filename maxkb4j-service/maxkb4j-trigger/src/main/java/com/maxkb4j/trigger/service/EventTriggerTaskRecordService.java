package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class EventTriggerTaskRecordService extends ServiceImpl<EventTriggerTaskRecordMapper, EventTriggerTaskRecordEntity> implements IEventTriggerTaskRecordService {
}
