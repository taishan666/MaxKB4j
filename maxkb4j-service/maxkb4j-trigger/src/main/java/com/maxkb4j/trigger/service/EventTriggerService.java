package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
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
    // 常量定义
    private static final String SOURCE_TYPE_APPLICATION = "APPLICATION";
    private static final String DEFAULT_WORKSPACE_ID = "default";
    private static final String TRIGGER_TYPE_SCHEDULED = "SCHEDULED";
    private static final String SCHEDULE_TYPE_DAILY = "daily";
    private static final String SCHEDULE_TYPE_WEEKLY = "weekly";
    private static final String SCHEDULE_TYPE_MONTHLY = "monthly";
    private static final String INTERVAL = "interval";
    private final IEventTriggerTaskService eventTriggerTaskService;
    private final IUserService userService;
    private final IApplicationService applicationService;
    private final IToolService toolService;

    @Override
    public IPage<EventTriggerEntity> pageList(int current, int size, EventQuery query) {       
        IPage<EventTriggerEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<EventTriggerEntity> wrapper = Wrappers.lambdaQuery();
        
        if (query != null) {
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
        }
        
        wrapper.orderByDesc(EventTriggerEntity::getCreateTime);
        IPage<EventTriggerEntity> res = this.page(page, wrapper);
        
        Map<String, String> nicknameMap = userService.getNicknameMap();
        List<EventTriggerEntity> records = res.getRecords();
        
        if (records == null || records.isEmpty()) {
            return res;
        }
        
        // 批量查询所有trigger对应的task
        List<String> triggerIds = records.stream()
                .filter(Objects::nonNull)
                .map(EventTriggerEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        
        if (triggerIds.isEmpty()) {
            return res;
        }
        
        LambdaQueryWrapper<EventTriggerTaskEntity> taskWrapper = Wrappers.lambdaQuery();
        taskWrapper.in(EventTriggerTaskEntity::getTriggerId, triggerIds);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(taskWrapper);
        
        if (allTasks == null || allTasks.isEmpty()) {
            // 没有任务，直接设置昵称并返回
            records.forEach(eventTriggerEntity -> eventTriggerEntity.setCreateUser(nicknameMap.get(eventTriggerEntity.getUserId())));
            return res;
        }
        
        // 分组整理tasks
        Map<String, List<EventTriggerTaskEntity>> taskMap = allTasks.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(EventTriggerTaskEntity::getTriggerId));
        
        // 提取所有需要查询的application和tool的id
        List<String> appIds = allTasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> SOURCE_TYPE_APPLICATION.equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        
        List<String> toolIds = allTasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> !SOURCE_TYPE_APPLICATION.equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        
        // 批量查询application和tool
        Map<String, Object> appMap = new HashMap<>();
        if (!appIds.isEmpty()) {
            List<Map<String, Object>> apps = applicationService.listMaps(Wrappers.lambdaQuery(ApplicationEntity.class).in(ApplicationEntity::getId, appIds));
            if (apps != null) {
                apps.forEach(app -> {
                    Object id = app.get("id");
                    if (id != null) {
                        appMap.put(id.toString(), app);
                    }
                });
            }
        }
        
        Map<String, Object> toolMap = new HashMap<>();
        if (!toolIds.isEmpty()) {
            List<Map<String, Object>> tools = toolService.listMaps(Wrappers.lambdaQuery(ToolEntity.class).in(ToolEntity::getId, toolIds));
            if (tools != null) {
                tools.forEach(tool -> {
                    Object id = tool.get("id");
                    if (id != null) {
                        toolMap.put(id.toString(), tool);
                    }
                });
            }
        }
        // 处理数据
        records.forEach(eventTriggerEntity -> {
            List<EventTriggerTaskEntity> triggerTasks = taskMap.getOrDefault(eventTriggerEntity.getId(), new ArrayList<>());
            TaskProcessResult result = processTaskList(triggerTasks, appMap, toolMap);
            List<EventTriggerTaskEntity> processedTasks = result.getTasks();
            String taskStr = result.getTaskString();
            // 计算下次执行时间
            if (eventTriggerEntity.getTriggerType().equals("SCHEDULED")) {
                String nextRunTime = calculateNextRunTime(eventTriggerEntity.getTriggerSetting());
                if (StringUtils.isNotBlank(nextRunTime)) {
                    eventTriggerEntity.setNextRunTime(nextRunTime);
                }
            }
            eventTriggerEntity.setTriggerTask(processedTasks);
            eventTriggerEntity.setTriggerTaskStr(taskStr);
            eventTriggerEntity.setCreateUser(nicknameMap.get(eventTriggerEntity.getUserId()));
        });
        return res;
    }

    /**
     * 处理任务列表，设置任务名称、图标等信息
     */
    private TaskProcessResult processTaskList(List<EventTriggerTaskEntity> tasks, Map<String, Object> appMap, Map<String, Object> toolMap) {
        StringBuilder taskStrBuilder = new StringBuilder();
        List<EventTriggerTaskEntity> processedTasks = new ArrayList<>();

        for (EventTriggerTaskEntity task : tasks) {
            EventTriggerTaskEntity processedTask = new EventTriggerTaskEntity();
            BeanUtil.copyProperties(task, processedTask);
            processedTask.setType(task.getSourceType());

            if (SOURCE_TYPE_APPLICATION.equals(task.getSourceType())) {
                Map<String, Object> app = (Map<String, Object>) appMap.get(task.getSourceId());
                if (app != null) {
                    processedTask.setIcon(app.get("icon") != null ? app.get("icon").toString() : "");
                    processedTask.setName(app.get("name") != null ? app.get("name").toString() : "");
                    taskStrBuilder.append(" " + app.get("name"));
                }
            } else {
                Map<String, Object> tool = (Map<String, Object>) toolMap.get(task.getSourceId());
                if (tool != null) {
                    processedTask.setIcon(tool.get("icon") != null ? tool.get("icon").toString() : "");
                    processedTask.setName(tool.get("name") != null ? tool.get("name").toString() : "");
                    taskStrBuilder.append(" " + tool.get("name"));
                }
            }
            processedTasks.add(processedTask);
        }

        return new TaskProcessResult(processedTasks, taskStrBuilder.toString().trim());
    }

    /**
     * 任务处理结果封装类
     */
    private static class TaskProcessResult {
        private final List<EventTriggerTaskEntity> tasks;
        private final String taskString;

        public TaskProcessResult(List<EventTriggerTaskEntity> tasks, String taskString) {
            this.tasks = tasks;
            this.taskString = taskString;
        }

        public List<EventTriggerTaskEntity> getTasks() {
            return tasks;
        }

        public String getTaskString() {
            return taskString;
        }
    }

    /**
     * 计算下次执行时间
     */
    private String calculateNextRunTime(Object triggerSettingObj) {
        if (!(triggerSettingObj instanceof Map)) {
            return null;
        }

        Map<String, Object> triggerSetting = (Map<String, Object>) triggerSettingObj;
        String scheduleType = (String) triggerSetting.get("scheduleType");
        List<String> timeList = getStringList(triggerSetting.get("time"));

        if (timeList == null || timeList.isEmpty()) {
            return null;
        }

        String timeStr = timeList.get(0); // 取第一个时间
        String[] timeParts = timeStr.split(":");
        if (timeParts.length < 2) {
            return null;
        }

        try {
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            switch (scheduleType) {
                case SCHEDULE_TYPE_DAILY:
                    return DateTimeUtil.getNextDayAtTime(hour, minute, 0).toString();
                case SCHEDULE_TYPE_WEEKLY:
                    List<String> dayList = getStringList(triggerSetting.get("days"));
                    if (dayList == null || dayList.isEmpty()) {
                        return null;
                    }
                    int day = Integer.parseInt(dayList.get(0));
                    return DateTimeUtil.getSameDayNextWeek(day, hour, minute, 0).toString();
                case SCHEDULE_TYPE_MONTHLY:
                    List<String> monthDayList = getStringList(triggerSetting.get("days"));
                    if (monthDayList == null || monthDayList.isEmpty()) {
                        return null;
                    }
                    int monthDay = Integer.parseInt(monthDayList.get(0));
                    return DateTimeUtil.getSameDayNextMonth(monthDay, hour, minute, 0).toString();
                case INTERVAL:
                    String intervalValue = triggerSetting.get("intervalValue").toString();
                    String intervalUnit = (String) triggerSetting.get("intervalUnit");
                    return DateTimeUtil.getSameDayNextInterval(intervalValue, intervalUnit,hour, minute, 0).toString();
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            // 处理数字格式异常
            return null;
        }
    }

    /**
     * 安全地将对象转换为字符串列表
     */
    private List<String> getStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List<?>) {
            for (Object item : (List<?>) obj) {
                if (item instanceof String) {
                    result.add((String) item);
                } else if (item instanceof Integer) {
                    result.add(item.toString());
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public boolean saveTrigger(EventTriggerEntity dto, Boolean isEdit) {
        if (dto == null) {
            return false;
        }
        
        dto.setUserId(StpKit.ADMIN.getLoginIdAsString());
        Date now = new Date();
        boolean isEditValue = Boolean.TRUE.equals(isEdit);
        
        if (!isEditValue) {
            dto.setId(null);
            dto.setIsActive(false);
            dto.setCreateTime(now);
        }
        
        dto.setWorkspaceId(DEFAULT_WORKSPACE_ID);
        dto.setUpdateTime(now);
        this.saveOrUpdate(dto);
        
        if (!isEditValue) {
            LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
            wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, dto.getId());
            dto.setTriggerTask(eventTriggerTaskService.list(wrapperTask));
        }
        
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

        return true;
    }

    @Override
    @Transactional
    public boolean batchActivate(String id, Boolean isActive) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        
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
        
        if (taskEntities != null && !taskEntities.isEmpty()) {
            Date now = new Date();
            taskEntities.forEach(task -> {
                task.setIsActive(isActive);
                task.setUpdateTime(now);
            });
            eventTriggerTaskService.updateBatchById(taskEntities);
        }
        
        return true;
    }

    @Override
    @Transactional
    public boolean batchDelete(String id) {
        if (StringUtils.isBlank(id)) {
            return false;
        }
        
        this.removeById(id);
        
        // 同时删除关联的任务
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        eventTriggerTaskService.remove(wrapperTask);
        
        return true;
    }

    @Override
    public EventTriggerEntity getDetailById(String id) {
        EventTriggerEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }

        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);

        // 提取所有应用程序ID和工具ID
        List<String> appIds = allTasks.stream()
                .filter(task -> "APPLICATION".equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .distinct()
                .toList();
        List<String> toolIds = allTasks.stream()
                .filter(task -> !"APPLICATION".equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .distinct()
                .toList();

        // 批量查询应用程序和工具
        Map<String, ApplicationEntity> appMap = new HashMap<>();
        if (!appIds.isEmpty()) {
            List<ApplicationEntity> apps = applicationService.listByIds(appIds);
            appMap = apps.stream().collect(Collectors.toMap(ApplicationEntity::getId, app -> app));
        }

        Map<String, ToolEntity> toolMap = new HashMap<>();
        if (!toolIds.isEmpty()) {
            List<ToolEntity> tools = toolService.listByIds(toolIds);
            toolMap = tools.stream().collect(Collectors.toMap(ToolEntity::getId, tool -> tool));
        }

        // 处理任务列表
        StringBuilder taskStrBuilder = new StringBuilder();
        List<EventTriggerTaskEntity> resTask = new ArrayList<>();
        List<ApplicationEntity> appsList = new ArrayList<>();
        List<ToolEntity> toolsList = new ArrayList<>();

        for (EventTriggerTaskEntity task : allTasks) {
            EventTriggerTaskEntity newTask = BeanUtil.copy(task, EventTriggerTaskEntity.class);
            newTask.setType(task.getSourceType());

            if (SOURCE_TYPE_APPLICATION.equals(task.getSourceType())) {
                ApplicationEntity app = appMap.get(task.getSourceId());
                if (app != null) {
                    appsList.add(app);
                    newTask.setIcon(app.getIcon());
                    newTask.setName(app.getName());
                    taskStrBuilder.append(" " + app.getName());
                }
            } else {
                ToolEntity tool = toolMap.get(task.getSourceId());
                if (tool != null) {
                    toolsList.add(tool);
                    newTask.setIcon(tool.getIcon());
                    newTask.setName(tool.getName());
                    taskStrBuilder.append(" " + tool.getName());
                }
            }
            resTask.add(newTask);
        }

        entity.setApplicationTaskList(appsList);
        entity.setToolTaskList(toolsList);
        entity.setTriggerTask(resTask);
        entity.setTriggerTaskStr(taskStrBuilder.toString().trim());
        return entity;
    }

    @Override
    public List<EventTriggerEntity> listBySource(String sourceType, String sourceId) {
        if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(sourceId)) {
            return List.of();
        }
        
        //TODO: 实现根据sourceType和sourceId查询触发器列表的逻辑
        return List.of();
    }

    @Override
    public List<EventTriggerEntity> getTriggerList(String id) {
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getSourceId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);
        List<EventTriggerEntity> resTask = new ArrayList<>();
        for (EventTriggerTaskEntity task : allTasks) {
            EventTriggerEntity  newTask = this.getById(task.getTriggerId()) ;
            resTask.add(newTask);
        }
        return resTask;
    }


}
