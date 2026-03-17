package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.tool.vo.ToolVO;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.mapper.EventTriggerMapper;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EventTriggerService extends ServiceImpl<EventTriggerMapper, EventTriggerEntity> implements IEventTriggerService {
    private final IEventTriggerTaskService eventTriggerTaskService;
    private final IUserService userService;
    private final IApplicationService applicationService;
    private final IToolService toolService;

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
        Map<String, String> nicknameMap = userService.getNicknameMap();
        List<EventTriggerEntity> records = res.getRecords();
        if (records.isEmpty()) {
            return res;
        }
        // 批量查询所有trigger对应的task
        List<String> triggerIds = records.stream().map(EventTriggerEntity::getId).toList();
        LambdaQueryWrapper<EventTriggerTaskEntity> taskWrapper = Wrappers.lambdaQuery();
        taskWrapper.in(EventTriggerTaskEntity::getTriggerId, triggerIds);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(taskWrapper);
        // 分组整理tasks
        Map<String, List<EventTriggerTaskEntity>> taskMap = allTasks.stream().collect(Collectors.groupingBy(EventTriggerTaskEntity::getTriggerId));
        // 提取所有需要查询的application和tool的id
        List<String> appIds = allTasks.stream().filter(task -> "APPLICATION".equals(task.getSourceType())).map(EventTriggerTaskEntity::getSourceId).distinct().toList();
        List<String> toolIds = allTasks.stream().filter(task -> !"APPLICATION".equals(task.getSourceType())).map(EventTriggerTaskEntity::getSourceId).distinct().toList();
        // 批量查询application和tool
        Map<String, Object> appMap = new HashMap<>();
        if (!appIds.isEmpty()) {
            List<Map<String, Object>> apps = applicationService.listMaps(Wrappers.lambdaQuery(ApplicationEntity.class).in(ApplicationEntity::getId, appIds));
            apps.forEach(app -> {
                Object id = app.get("id");
                if (id != null) {
                    appMap.put(id.toString(), app);
                }
            });
        }
        Map<String, Object> toolMap = new HashMap<>();
        if (!toolIds.isEmpty()) {
            List<Map<String, Object>> tools = toolService.listMaps(Wrappers.lambdaQuery(ToolEntity.class).in(ToolEntity::getId, toolIds));
            tools.forEach(tool -> {
                Object id = tool.get("id");
                if (id != null) {
                    toolMap.put(id.toString(), tool);
                }
            });
        }
        // 处理数据
        records.forEach(eventTriggerEntity -> {
            List<EventTriggerTaskEntity> triggerTasks = taskMap.getOrDefault(eventTriggerEntity.getId(), new ArrayList<>());
            StringBuilder taskStrBuilder = new StringBuilder();
            triggerTasks.forEach(task -> {
                task.setType(task.getSourceType());
                if ("APPLICATION".equals(task.getSourceType())) {
                    Map<String, Object> app = (Map<String, Object>) appMap.get(task.getSourceId());
                    if (app != null) {
                        task.setIcon(app.get("icon") != null ? app.get("icon").toString() : "");
                        task.setName(app.get("name") != null ? app.get("name").toString() : "");
                        task.setType(task.getSourceType());
                        taskStrBuilder.append(" " + app.get("name"));
                    }
                } else {
                    Map<String, Object> tool = (Map<String, Object>) toolMap.get(task.getSourceId());
                    if (tool != null) {
                        task.setIcon(tool.get("icon") != null ? tool.get("icon").toString() : "");
                        task.setName(tool.get("name") != null ? tool.get("name").toString() : "");
                        task.setType(task.getSourceType());
                        taskStrBuilder.append(" " + tool.get("name"));
                    }
                }
            });
            eventTriggerEntity.setTriggerTask(triggerTasks);
            eventTriggerEntity.setTriggerTaskStr(taskStrBuilder.toString().trim());
            eventTriggerEntity.setCreateUser(nicknameMap.get(eventTriggerEntity.getUserId()));
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
        String DEFAULT_ID = "default";
        dto.setWorkspaceId(DEFAULT_ID);
        dto.setUpdateTime(now);
        this.saveOrUpdate(dto);
        if (!isEdit) {
            LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
            wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, dto.getId());
            dto.setTriggerTask(eventTriggerTaskService.list(wrapperTask));
        }
        if (dto.getTriggerTask() != null) {
            LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
            wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, dto.getId());
            eventTriggerTaskService.remove(wrapperTask);
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
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);
        StringBuilder taskStrBuilder = new StringBuilder();
        List<EventTriggerTaskEntity> resTask = new ArrayList<>();
        List<ApplicationEntity> apps = new ArrayList<>();
        List<ToolEntity> tools = new ArrayList<>();
        allTasks.forEach(task -> {
            var newTask = new EventTriggerTaskEntity();
            if (task.getSourceType().equals("APPLICATION")) {
                var app = applicationService.getById(task.getSourceId());
                apps.add(app);
                newTask = BeanUtil.copy(task, EventTriggerTaskEntity.class);
                newTask.setIcon(app.getIcon());
                newTask.setName(app.getName());
                taskStrBuilder.append(" " + app.getName());
                newTask.setType(task.getSourceType());
            } else {
                var tool = toolService.getById(task.getSourceId());
                tools.add(tool);
                newTask = BeanUtil.copy(task, EventTriggerTaskEntity.class);
                newTask.setIcon(tool.getIcon());
                newTask.setName(tool.getName());
                taskStrBuilder.append(" " + tool.getName());
                newTask.setType(task.getSourceType());
            }
            resTask.add(newTask);
        });
        entity.setApplicationTaskList(apps);
        entity.setToolTaskList(tools);
        entity.setTriggerTask(resTask);
        entity.setTriggerTaskStr(taskStrBuilder.toString().trim());
        return entity;
    }

    @Override
    public List<EventTriggerEntity> listBySource(String sourceType, String sourceId) {
        //TODO
        return List.of();
    }


}
