package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.mapper.EventTriggerMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class EventTriggerService extends ServiceImpl<EventTriggerMapper, EventTriggerEntity> implements IEventTriggerService{
    @Override
    public IPage<EventTriggerEntity> pageList(int current, int size, EventQuery query) {
        IPage<EventTriggerEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<EventTriggerEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(EventTriggerEntity::getName, query.getName());
        }
        if (StringUtils.isNotBlank(query.getCreateUser())) {
            wrapper.eq(EventTriggerEntity::getUserId, query.getCreateUser());
        }
        if (StringUtils.isNotBlank(query.getTriggerType())) {
            wrapper.eq(EventTriggerEntity::getTriggerType, query.getTriggerType());
        }
        if (Objects.nonNull(query.getIsActive())) {
            wrapper.eq(EventTriggerEntity::getIsActive, query.getIsActive());
        }
        return this.page(page, wrapper);
    }
}
