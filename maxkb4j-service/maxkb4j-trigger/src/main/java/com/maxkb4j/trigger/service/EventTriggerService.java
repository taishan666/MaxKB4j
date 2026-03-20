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
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.enums.TriggerType;
import com.maxkb4j.trigger.mapper.EventTriggerMapper;
import com.maxkb4j.trigger.vo.EventTriggerTaskVO;
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
        Page<EventTriggerVO> page = new Page<>(current, size);
        IPage<EventTriggerVO> pageList = this.baseMapper.selectEventTriggerPage(page, query);
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
        List<EventTriggerTaskVO> allTasks = BeanUtil.copyList(eventTriggerTaskService.list(taskWrapper),EventTriggerTaskVO.class);
        // 分组整理tasks
        Map<String, List<EventTriggerTaskVO>> taskMap = allTasks.stream().collect(Collectors.groupingBy(EventTriggerTaskVO::getTriggerId));
        // 处理数据
        records.forEach(eventTrigger -> {
            List<EventTriggerTaskVO> triggerTasks = taskMap.getOrDefault(eventTrigger.getId(), List.of());
            EventTriggerTaskProcessor.PageResult result = taskProcessor.processForPage(triggerTasks);
            if (TriggerType.SCHEDULED.name().equals(eventTrigger.getTriggerType())) {
                String nextRunTime = nextRunTimeCalculator.calculateStr(eventTrigger.getTriggerSetting());
                if (StringUtils.isNotBlank(nextRunTime)) {
                    eventTrigger.setNextRunTime(nextRunTime);
                }
            }
            eventTrigger.setTriggerTask(result.tasks());
            eventTrigger.setCreateUser(nicknameMap.get(eventTrigger.getUserId()));
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
        // 先查询触发器类型，避免更新后再查询
        EventTriggerEntity trigger = this.getById(id);
        if (trigger == null) {
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
        if (TriggerType.SCHEDULED.name().equals(trigger.getTriggerType())) {
            trigger.setIsActive(isActive);
            if (Boolean.TRUE.equals(isActive)) {
                triggerSchedulerProvider.getObject().scheduleTrigger(trigger);
            } else {
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
        EventTriggerTaskProcessor.DetailResult result = taskProcessor.processForDetail(BeanUtil.copyList(allTasks, EventTriggerTaskVO.class));
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