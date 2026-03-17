package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.mapper.EventTriggerMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class EventTriggerService extends ServiceImpl<EventTriggerMapper, EventTriggerEntity> implements IEventTriggerService {
    private final IEventTriggerTaskService eventTriggerTaskService;
    private final String DEFAULT_ID = "default";

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
        wrapper.orderByDesc(EventTriggerEntity::getCreateTime);
        IPage<EventTriggerEntity> res = this.page(page, wrapper);
        res.getRecords().forEach(eventTriggerEntity -> {
            List<EventTriggerTaskEntity> eventTriggerTaskEntities = new ArrayList<>();
            LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
            wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, eventTriggerEntity.getId());
            eventTriggerTaskEntities = eventTriggerTaskService.list(wrapperTask);
            eventTriggerEntity.setTriggerTask(eventTriggerTaskEntities);
        });
        return res;
    }

    @Override
    @Transactional
    public boolean saveTigger(EventTriggerEntity dto, Boolean isEdit) {
        dto.setUserId(StpKit.ADMIN.getLoginIdAsString());
        Date now = new Date();
        if (!isEdit) {
            dto.setId(null);
            dto.setIsActive(false);
            dto.setCreateTime(now);
        }
        dto.setWorkspaceId(DEFAULT_ID);
        dto.setUpdateTime(now);
        this.saveOrUpdate(dto);
        if (!isEdit) {
            LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
            wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, dto.getId());
            dto.setTriggerTask(eventTriggerTaskService.list(wrapperTask));
        }
        if (dto.getTriggerTask() != null) {
            List<EventTriggerTaskEntity> resList = new ArrayList<>();
            dto.getTriggerTask().forEach(item -> {
                EventTriggerTaskEntity ett = new EventTriggerTaskEntity();
                if (!isEdit) {
                    ett.setId(null);
                    ett.setCreateTime(now);
                }
                ett.setTriggerId(dto.getId());
                ett.setIsActive(dto.getIsActive());
                ett.setSourceType(item.getSourceType());
                ett.setSourceId(item.getSourceId());
                ett.setParameter(item.getParameter());
                ett.setMeta(dto.getMeta());
                ett.setUpdateTime(now);
                resList.add(ett);
            });
            eventTriggerTaskService.saveOrUpdateBatch(resList);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean batchActivate(String id, Boolean isActive) {
        EventTriggerEntity entity = this.getById(id);
        if (entity == null) {
            return false;
        }
        entity.setIsActive(isActive);
        entity.setUpdateTime(new Date());
        this.updateById(entity);
        // 同时启用关联的任务
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> taskEntities = eventTriggerTaskService.list(wrapperTask);
        taskEntities.forEach(task -> {
            task.setIsActive(isActive);
            task.setUpdateTime(new Date());
        });
        eventTriggerTaskService.updateBatchById(taskEntities);
        return true;
    }

    @Override
    public boolean batchDelete(String id) {
        this.removeById(id);
        // 同时启用关联删除
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        eventTriggerTaskService.remove(wrapperTask);
        return true;
    }

    @Override
    public EventTriggerEntity getDetailById(String id) {
        EventTriggerEntity entity = this.getById(id);
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, entity.getId());
        entity.setTriggerTask(eventTriggerTaskService.list(wrapperTask));
        return entity;
    }


}
