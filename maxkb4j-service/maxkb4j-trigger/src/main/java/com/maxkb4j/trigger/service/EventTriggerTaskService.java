package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class EventTriggerTaskService extends ServiceImpl<EventTriggerTaskMapper, EventTriggerTaskEntity> implements IEventTriggerTaskService {
    @Override
    public IPage<EventTriggerTaskEntity> pageList(String id, int current, int size, EventTaskQuery query) {
        return new Page<>(current, size);
    }
}
