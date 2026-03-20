package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.enums.TriggerType;
import com.maxkb4j.trigger.mapper.EventTriggerMapper;
import com.maxkb4j.trigger.vo.EventTriggerVO;
import com.maxkb4j.trigger.vo.SourceEventTriggerVO;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventTriggerService extends ServiceImpl<EventTriggerMapper, EventTriggerEntity> implements IEventTriggerService {

    private final IEventTriggerTaskService eventTriggerTaskService;
    private final IUserService userService;
    private final IApplicationService applicationService;
    private final IToolService toolService;
    private final NextRunTimeCalculator nextRunTimeCalculator;
    private final EventTriggerTaskProcessor taskProcessor;
    private final ObjectProvider<TriggerScheduler> triggerSchedulerProvider;

    @Override
    public IPage<EventTriggerVO> pageList(int current, int size, EventQuery query) {
        IPage<EventTriggerEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<EventTriggerEntity> wrapper = Wrappers.lambdaQuery();
        if (query != null) {
            if (StringUtils.isNotBlank(query.getName())) {
                wrapper.like(EventTriggerEntity::getName, query.getName());
            }
            if (StringUtils.isNotBlank(query.getCreateUser())) {
                wrapper.eq(EventTriggerEntity::getUserId, query.getCreateUser());
            }
            if (StringUtils.isNotBlank(query.getType())) {
                wrapper.eq(EventTriggerEntity::getTriggerType, query.getType());
            }
            if (Objects.nonNull(query.getIsActive())) {
                wrapper.eq(EventTriggerEntity::getIsActive, query.getIsActive());
            }
        }
        wrapper.orderByDesc(EventTriggerEntity::getCreateTime);
        IPage<EventTriggerVO> pageList = PageUtil.copy(this.page(page, wrapper), EventTriggerVO.class);
        Map<String, String> nicknameMap = userService.getNicknameMap();
        List<EventTriggerVO> records = pageList.getRecords();
        if (records == null || records.isEmpty()) {
            return pageList;
        }
        // 批量查询所有trigger对应的task
        List<String> triggerIds = records.stream().map(EventTriggerEntity::getId).toList();
        if (triggerIds.isEmpty()) {
            return pageList;
        }
        LambdaQueryWrapper<EventTriggerTaskEntity> taskWrapper = Wrappers.lambdaQuery();
        taskWrapper.in(EventTriggerTaskEntity::getTriggerId, triggerIds);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(taskWrapper);
        // 分组整理tasks
        Map<String, List<EventTriggerTaskEntity>> taskMap = allTasks.stream().collect(Collectors.groupingBy(EventTriggerTaskEntity::getTriggerId));
        // 处理数据
        records.forEach(eventTriggerEntity -> {
            List<EventTriggerTaskEntity> triggerTasks = taskMap.getOrDefault(eventTriggerEntity.getId(), List.of());
            EventTriggerTaskProcessor.PageResult result = taskProcessor.processForPage(triggerTasks);
            if (TriggerType.SCHEDULED.name().equals(eventTriggerEntity.getTriggerType())) {
                String nextRunTime = nextRunTimeCalculator.calculateStr(eventTriggerEntity.getTriggerSetting());
                if (StringUtils.isNotBlank(nextRunTime)) {
                    eventTriggerEntity.setNextRunTime(nextRunTime);
                }
            }
            eventTriggerEntity.setTriggerTask(result.tasks());
            eventTriggerEntity.setCreateUser(nicknameMap.get(eventTriggerEntity.getUserId()));
        });
        return pageList;
    }

    @Override
    @Transactional
    public void saveTrigger(EventTriggerDTO dto, Boolean isEdit) {
        if (dto == null) {
            return;
        }
        if (TriggerType.SCHEDULED.name().equals(dto.getTriggerType())) {
            JSONObject triggerSetting = dto.getTriggerSetting();
            if (triggerSetting == null || !triggerSetting.containsKey("scheduleType")) {
                throw new ApiException("请选择触发周期");
            }
        }
        dto.setUserId(StpKit.ADMIN.getLoginIdAsString());
        Date now = new Date();
        boolean isEditValue = Boolean.TRUE.equals(isEdit);
        if (!isEditValue) {
            dto.setId(null);
            dto.setIsActive(false);
            dto.setCreateTime(now);
        }
        dto.setWorkspaceId(AppConst.DEFAULT_WORKSPACE_ID);
        dto.setUpdateTime(now);
        this.saveOrUpdate(dto);
        if (dto.getTriggerTask() != null) {
            LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
            wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, dto.getId());
            eventTriggerTaskService.remove(wrapperTask);
            List<EventTriggerTaskEntity> resList = new ArrayList<>();
            for (EventTriggerTaskEntity item : dto.getTriggerTask()) {
                if (item == null || StringUtils.isBlank(item.getSourceId())) {
                    continue;
                }
                EventTriggerTaskEntity ett = new EventTriggerTaskEntity();
                if (!isEditValue) {
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
            }

            if (!resList.isEmpty()) {
                eventTriggerTaskService.saveOrUpdateBatch(resList);
            }
        }
        EventTriggerEntity savedTrigger = this.getById(dto.getId());
        // 重新调度定时触发器
        if (TriggerType.SCHEDULED.name().equals(savedTrigger.getTriggerType())) {
            triggerSchedulerProvider.getObject().rescheduleTrigger(savedTrigger);
        }
    }

    @Override
    @Transactional
    public boolean batchActivate(String id, Boolean isActive) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        Date now = new Date();
        // 直接更新触发器状态
        LambdaUpdateWrapper<EventTriggerEntity> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(EventTriggerEntity::getId, id).set(EventTriggerEntity::getIsActive, isActive).set(EventTriggerEntity::getUpdateTime, now);
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            return false;
        }
        // 同时更新关联任务的状态
        LambdaUpdateWrapper<EventTriggerTaskEntity> taskUpdateWrapper = Wrappers.lambdaUpdate();
        taskUpdateWrapper.eq(EventTriggerTaskEntity::getTriggerId, id).set(EventTriggerTaskEntity::getIsActive, isActive).set(EventTriggerTaskEntity::getUpdateTime, now);
        eventTriggerTaskService.update(taskUpdateWrapper);
        // 更新调度状态
        EventTriggerEntity trigger = this.getById(id);
        log.info("batchActivate: id={}, isActive={}, trigger={}", id, isActive, trigger);
        if (trigger != null && TriggerType.SCHEDULED.name().equals(trigger.getTriggerType())) {
            // 手动设置 isActive 确保使用最新值（避免缓存问题）
            trigger.setIsActive(isActive);
            if (Boolean.TRUE.equals(isActive)) {
                triggerSchedulerProvider.getObject().scheduleTrigger(trigger);
            } else {
                log.info("Calling cancelSchedule for trigger {}", id);
                triggerSchedulerProvider.getObject().cancelSchedule(id);
            }
        }

        return true;
    }

    @Override
    @Transactional
    public boolean deleteTrigger(String id) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        // 取消调度
        triggerSchedulerProvider.getObject().cancelSchedule(id);
        this.removeById(id);
        // 同时删除关联的任务
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        eventTriggerTaskService.remove(wrapperTask);
        return true;
    }

    @Override
    public EventTriggerVO getDetailById(String id) {
        EventTriggerEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        EventTriggerVO vo = BeanUtil.copy(entity, EventTriggerVO.class);
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);
        EventTriggerTaskProcessor.DetailResult result = taskProcessor.processForDetail(allTasks);
        vo.setApplicationTaskList(result.apps());
        vo.setToolTaskList(result.tools());
        vo.setTriggerTask(result.tasks());
        return vo;
    }

    @Override
    public SourceEventTriggerVO getDetailBySourceId(String id, String sourceType, String sourceId) {
        EventTriggerEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        SourceEventTriggerVO vo = BeanUtil.copy(entity, SourceEventTriggerVO.class);
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);
        Optional<EventTriggerTaskEntity> sourceTask = allTasks.stream().filter(task -> sourceType.equals(task.getSourceType()) && sourceId.equals(task.getSourceId())).findFirst();
        if (sourceTask.isPresent()) {
            if (ResourceType.APPLICATION.equals(sourceType)) {
                vo.setApplicationTask(applicationService.getById(sourceTask.get().getSourceId()));
            } else if (ResourceType.TOOL.equals(sourceType)) {
                vo.setToolTask(toolService.getById(sourceTask.get().getSourceId()));
            }
            vo.setTriggerTask(sourceTask.get());
        }
        return vo;
    }

    @Override
    public List<EventTriggerEntity> listBySource(String sourceType, String sourceId) {
        if (StringUtils.isBlank(sourceId)) {
            return List.of();
        }
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getSourceType, sourceType);
        wrapperTask.eq(EventTriggerTaskEntity::getSourceId, sourceId);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);
        if (allTasks == null || allTasks.isEmpty()) {
            return List.of();
        }
        // 批量查询避免N+1问题
        List<String> triggerIds = allTasks.stream().map(EventTriggerTaskEntity::getTriggerId).distinct().toList();
        return this.listByIds(triggerIds);
    }
}