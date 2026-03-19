package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.enums.ScheduleType;
import com.maxkb4j.trigger.enums.SourceType;
import com.maxkb4j.trigger.enums.TriggerType;
import com.maxkb4j.trigger.mapper.EventTriggerMapper;
import com.maxkb4j.trigger.vo.EventTriggerVO;
import com.maxkb4j.trigger.vo.SourceEventTriggerVO;
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
    private static final String DEFAULT_WORKSPACE_ID = "default";
    private final IEventTriggerTaskService eventTriggerTaskService;
    private final IUserService userService;
    private final IApplicationService applicationService;
    private final IToolService toolService;

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
        IPage<EventTriggerVO> res = PageUtil.copy(this.page(page, wrapper), EventTriggerVO.class);
        Map<String, String> nicknameMap = userService.getNicknameMap();
        List<EventTriggerVO> records =res.getRecords();
        if (records == null || records.isEmpty()) {
            return res;
        }
        // 批量查询所有trigger对应的task
        List<String> triggerIds = records.stream()
                .map(EventTriggerEntity::getId)
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
                .collect(Collectors.groupingBy(EventTriggerTaskEntity::getTriggerId));
        // 提取所有需要查询的application和tool的id
        List<String> appIds = allTasks.stream()
                .filter(task -> SourceType.APPLICATION.name().equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        List<String> toolIds = allTasks.stream()
                .filter(task -> SourceType.TOOL.name().equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        // 批量查询application和tool
        List<Map<String, Object>>  apps = CollectionUtils.isEmpty(appIds)? List.of() : applicationService.listMaps(Wrappers.lambdaQuery(ApplicationEntity.class).in(ApplicationEntity::getId, appIds));
        List<Map<String, Object>>  tools = CollectionUtils.isEmpty(toolIds)? List.of() :toolService.listMaps(Wrappers.lambdaQuery(ToolEntity.class).in(ToolEntity::getId, toolIds));
        // 处理数据
        records.forEach(eventTriggerEntity -> {
            List<EventTriggerTaskEntity> triggerTasks = taskMap.getOrDefault(eventTriggerEntity.getId(), List.of());
            TaskProcessResult result = processTaskList(triggerTasks, buildSourceMap(apps),  buildSourceMap(tools));
            if (TriggerType.SCHEDULED.name().equals(eventTriggerEntity.getTriggerType())) {
                String nextRunTime = calculateNextRunTime(eventTriggerEntity.getTriggerSetting());
                if (StringUtils.isNotBlank(nextRunTime)) {
                    eventTriggerEntity.setNextRunTime(nextRunTime);
                }
            }
            eventTriggerEntity.setTriggerTask(result.tasks());
            eventTriggerEntity.setTriggerTaskStr(result.taskString());
            eventTriggerEntity.setCreateUser(nicknameMap.get(eventTriggerEntity.getUserId()));
        });
        return res;
    }

    /**
     * 根据ID列表构建源数据Map
     */
    private  Map<String, Map<String, Object>> buildSourceMap(List<Map<String, Object>> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new HashMap<>();
        list.forEach(item -> {
            Object id = item.get("id");
            if (id != null) {
                result.put(String.valueOf(id), item);
            }
        });
        return result;
    }

    /**
     * 处理任务列表，设置任务名称、图标等信息
     */
    private TaskProcessResult processTaskList(List<EventTriggerTaskEntity> tasks, Map<String, Map<String, Object>> appMap, Map<String, Map<String, Object>> toolMap) {
        StringBuilder taskStrBuilder = new StringBuilder();
        List<EventTriggerTaskEntity> processedTasks = new ArrayList<>();

        for (EventTriggerTaskEntity task : tasks) {
            EventTriggerTaskEntity processedTask = new EventTriggerTaskEntity();
            BeanUtil.copyProperties(task, processedTask);
            processedTask.setType(task.getSourceType());

            Map<String, Map<String, Object>> sourceMap = SourceType.APPLICATION.name().equals(task.getSourceType()) ? appMap : toolMap;
            Map<String, Object> source = sourceMap.get(task.getSourceId());
            if (source != null) {
                processedTask.setIcon(source.get("icon") != null ? source.get("icon").toString() : "");
                processedTask.setName(source.get("name") != null ? source.get("name").toString() : "");
                taskStrBuilder.append(" ").append(source.get("name"));
            }
            processedTasks.add(processedTask);
        }
        return new TaskProcessResult(processedTasks, taskStrBuilder.toString().trim());
    }

    /**
     * 任务处理结果封装类
     */
    private record TaskProcessResult(List<EventTriggerTaskEntity> tasks, String taskString) {}

    /**
     * 计算下次执行时间
     */
    @SuppressWarnings("unchecked")
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
            switch (ScheduleType.fromValue(scheduleType)) {
                case DAILY:
                    return DateTimeUtil.getNextDayAtTime(hour, minute, 0).toString();
                case WEEKLY:
                    List<String> dayList = getStringList(triggerSetting.get("days"));
                    if (dayList == null || dayList.isEmpty()) {
                        return null;
                    }
                    int day = Integer.parseInt(dayList.get(0));
                    return DateTimeUtil.getSameDayNextWeek(day, hour, minute, 0).toString();
                case MONTHLY:
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
        if (!(obj instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(item -> item instanceof String s ? s : item != null ? item.toString() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public void saveTrigger(EventTriggerDTO dto, Boolean isEdit) {
        if (dto == null) {
            return;
        }
        if (TriggerType.SCHEDULED.name().equals(dto.getTriggerType())){
            JSONObject triggerSetting=dto.getTriggerSetting();
            if (triggerSetting == null||!triggerSetting.containsKey("scheduleType")){
                throw new ApiException( "请选择触发周期");
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
        dto.setWorkspaceId(DEFAULT_WORKSPACE_ID);
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
    public EventTriggerVO getDetailById(String id) {
        EventTriggerEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        EventTriggerVO vo = BeanUtil.copy(entity, EventTriggerVO.class);
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);

        // 提取所有应用程序ID和工具ID
        List<String> appIds = allTasks.stream()
                .filter(task -> SourceType.APPLICATION.name().equals(task.getSourceType()))
                .map(EventTriggerTaskEntity::getSourceId)
                .distinct()
                .toList();
        List<String> toolIds = allTasks.stream()
                .filter(task -> SourceType.TOOL.name().equals(task.getSourceType()))
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
            if (SourceType.APPLICATION.name().equals(task.getSourceType())) {
                ApplicationEntity app = appMap.get(task.getSourceId());
                if (app != null) {
                    appsList.add(app);
                    newTask.setIcon(app.getIcon());
                    newTask.setName(app.getName());
                    taskStrBuilder.append(" ").append(app.getName());
                }
            } else {
                ToolEntity tool = toolMap.get(task.getSourceId());
                if (tool != null) {
                    toolsList.add(tool);
                    newTask.setIcon(tool.getIcon());
                    newTask.setName(tool.getName());
                    taskStrBuilder.append(" ").append(tool.getName());
                }
            }
            resTask.add(newTask);
        }
        vo.setApplicationTaskList(appsList);
        vo.setToolTaskList(toolsList);
        vo.setTriggerTask(resTask);
        vo.setTriggerTaskStr(taskStrBuilder.toString().trim());
        return vo;
    }

    @Override
    public SourceEventTriggerVO getDetailBySourceId(String id,String sourceType,String sourceId) {
        EventTriggerEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        SourceEventTriggerVO vo = BeanUtil.copy(entity, SourceEventTriggerVO.class);
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapperTask = Wrappers.lambdaQuery();
        wrapperTask.eq(EventTriggerTaskEntity::getTriggerId, id);
        List<EventTriggerTaskEntity> allTasks = eventTriggerTaskService.list(wrapperTask);
        Optional<EventTriggerTaskEntity> sourceTask = allTasks.stream()
                .filter(task -> sourceType.equals(task.getSourceType())&& sourceId.equals(task.getSourceId()))
                .findFirst();
        if (sourceTask.isPresent()){
            if (SourceType.APPLICATION.name().equals(sourceType)){
                vo.setApplicationTask(applicationService.getById(sourceTask.get().getSourceId()));
            }
            if (SourceType.TOOL.name().equals(sourceType)){
                vo.setApplicationTask(applicationService.getById(sourceTask.get().getSourceId()));
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
        List<String> triggerIds = allTasks.stream()
                .map(EventTriggerTaskEntity::getTriggerId)
                .distinct()
                .toList();
        return this.listByIds(triggerIds);
    }




}
