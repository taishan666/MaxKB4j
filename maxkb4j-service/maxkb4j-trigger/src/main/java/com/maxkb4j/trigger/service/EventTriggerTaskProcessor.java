package com.maxkb4j.trigger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.vo.EventTriggerTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 触发器任务处理器
 */
@Component
@RequiredArgsConstructor
public class EventTriggerTaskProcessor {

    private final IApplicationService applicationService;
    private final IToolService toolService;

    public PageResult processForPage(List<EventTriggerTaskVO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return new PageResult(List.of());
        }

        Map<String, Map<String, Object>> appMap = querySourceMap(tasks, ResourceType.APPLICATION);
        Map<String, Map<String, Object>> toolMap = querySourceMap(tasks, ResourceType.TOOL);

        return processTasks(tasks, appMap, toolMap);
    }

    public DetailResult processForDetail(List<EventTriggerTaskVO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return new DetailResult(List.of(), List.of(), List.of());
        }
        List<String> appIds = extractIds(tasks, ResourceType.APPLICATION);
        List<String> toolIds = extractIds(tasks, ResourceType.TOOL);

        List<ApplicationEntity> appsList = appIds.isEmpty() ? List.of() : applicationService.listByIds(appIds);
        List<ToolEntity> toolsList = toolIds.isEmpty() ? List.of() : toolService.listByIds(toolIds);

        Map<String, ApplicationEntity> appMap = appsList.stream()
                .collect(Collectors.toMap(ApplicationEntity::getId, a -> a));
        Map<String, ToolEntity> toolMap = toolsList.stream()
                .collect(Collectors.toMap(ToolEntity::getId, t -> t));

        List<EventTriggerTaskVO> result = tasks.stream()
                .map(task -> enrichTask(task, appMap, toolMap))
                .toList();

        return new DetailResult(result, appsList, toolsList);
    }

    private EventTriggerTaskVO enrichTask(EventTriggerTaskVO task,
                                          Map<String, ApplicationEntity> appMap,
                                          Map<String, ToolEntity> toolMap) {
        EventTriggerTaskVO newTask = BeanUtil.copy(task, EventTriggerTaskVO.class);
        newTask.setType(task.getSourceType());
        if (ResourceType.APPLICATION.equals(task.getSourceType())) {
            ApplicationEntity app = appMap.get(task.getSourceId());
            if (app != null) {
                newTask.setIcon(app.getIcon());
                newTask.setName(app.getName());
            }
        } else if (ResourceType.TOOL.equals(task.getSourceType())) {
            ToolEntity tool = toolMap.get(task.getSourceId());
            if (tool != null) {
                newTask.setIcon(tool.getIcon());
                newTask.setName(tool.getName());
            }
        }
        return newTask;
    }

    private Map<String, Map<String, Object>> querySourceMap(List<EventTriggerTaskVO> tasks, String type) {
        List<String> ids = extractIds(tasks, type);
        if (ids.isEmpty()) return Map.of();

        List<Map<String, Object>> results = Objects.equals(type, ResourceType.APPLICATION)
                ? applicationService.listMaps(new LambdaQueryWrapper<ApplicationEntity>().in(ApplicationEntity::getId, ids))
                : toolService.listMaps(new LambdaQueryWrapper<ToolEntity>().in(ToolEntity::getId, ids));

        if (results == null || results.isEmpty()) return Map.of();

        Map<String, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> item : results) {
            Object id = item.get("id");
            if (id != null) map.put(String.valueOf(id), item);
        }
        return map;
    }

    private List<String> extractIds(List<EventTriggerTaskVO> tasks, String type) {
        return tasks.stream()
                .filter(t -> type.equals(t.getSourceType()))
                .map(EventTriggerTaskVO::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private PageResult processTasks(List<EventTriggerTaskVO> tasks,
                                     Map<String, Map<String, Object>> appMap,
                                     Map<String, Map<String, Object>> toolMap) {
        List<EventTriggerTaskVO> result = tasks.stream()
                .map(task -> enrichTaskFromMaps(task, appMap, toolMap))
                .toList();
        return new PageResult(result);
    }

    private EventTriggerTaskVO enrichTaskFromMaps(EventTriggerTaskVO task,
                                                   Map<String, Map<String, Object>> appMap,
                                                   Map<String, Map<String, Object>> toolMap) {
        EventTriggerTaskVO newTask = BeanUtil.copy(task, EventTriggerTaskVO.class);
        newTask.setType(task.getSourceType());
        Map<String, Map<String, Object>> sourceMap = ResourceType.APPLICATION.equals(task.getSourceType())
                ? appMap : toolMap;
        Map<String, Object> source = sourceMap.get(task.getSourceId());

        if (source != null) {
            newTask.setIcon(source.get("icon") != null ? source.get("icon").toString() : "");
            newTask.setName(source.get("name") != null ? source.get("name").toString() : "");
        }
        return newTask;
    }

    public record PageResult(List<EventTriggerTaskVO> tasks) {}
    public record DetailResult(List<EventTriggerTaskVO> tasks, List<ApplicationEntity> apps, List<ToolEntity> tools) {}
}