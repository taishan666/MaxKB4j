package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.mapper.EventTriggerTaskRecordMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class EventTriggerTaskRecordService extends ServiceImpl<EventTriggerTaskRecordMapper, EventTriggerTaskRecordEntity> implements IEventTriggerTaskRecordService {
    @Override
    public IPage<EventTriggerTaskRecordEntity> pageList(String id, int current, int size, EventTaskQuery query) {
        IPage<EventTriggerTaskRecordEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<EventTriggerTaskRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(EventTriggerTaskRecordEntity::getTriggerId, id);
        if (query != null) {
            if (StringUtils.isNotBlank(query.getSourceType())) {
                wrapper.like(EventTriggerTaskRecordEntity::getSourceType, query.getSourceType());
            }
            if (StringUtils.isNotBlank(query.getState())) {
                wrapper.like(EventTriggerTaskRecordEntity::getState, query.getState());
            }
        }
        wrapper.orderByDesc(EventTriggerTaskRecordEntity::getCreateTime);
        return this.page(page, wrapper);
    }
}
