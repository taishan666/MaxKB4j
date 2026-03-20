package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.executor.GroovyScriptExecutor;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import com.maxkb4j.trigger.enums.TaskState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerTaskExecutor {

    private final IEventTriggerTaskService eventTriggerTaskService;
    private final IEventTriggerTaskRecordService eventTriggerTaskRecordService;
    private final IApplicationChatService applicationChatService;
    private final IToolService toolService;

    /**
     * 执行触发器的所有关联任务
     */
    public void execute(String triggerId) {
        execute(triggerId, new JSONObject());
    }

    public void execute(String triggerId,JSONObject data) {
        if (StringUtils.isBlank(triggerId)) {
            return;
        }
        log.info("Executing trigger tasks for triggerId: {}", triggerId);
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(EventTriggerTaskEntity::getTriggerId, triggerId);
        List<EventTriggerTaskEntity> tasks = eventTriggerTaskService.list(wrapper);
        if (tasks == null || tasks.isEmpty()) {
            log.info("No active tasks found for trigger: {}", triggerId);
            return;
        }
        for (EventTriggerTaskEntity task : tasks) {
            try {
                JSONObject parameter = task.getParameter();
                if (parameter != null) {
                    for (String key : parameter.keySet()) {
                        Object value = parameter.get(key);
                        if (value instanceof JSONObject) {
                            JSONObject fieldValue = parameter.getJSONObject(key);
                            if (fieldValue.containsKey("source")) {
                                String source = fieldValue.getString("source");
                                if ("reference".equals(source)) {
                                    List<String> reference = fieldValue.getObject("value", new TypeReference<List<String>>() {});
                                    parameter.put(key, data.get(reference.get(1)));
                                } else {
                                    parameter.put(key, fieldValue.get("value"));
                                }
                            }
                        }
                    }
                }
                executeTask(task);
            } catch (Exception e) {
                log.error("Error executing task {} for trigger {}: {}",
                        task.getId(), triggerId, e.getMessage(), e);
                saveRecord(triggerId, task.getId(), task.getSourceType(), task.getSourceId(), TaskState.FAILURE, 0f, new JSONObject());
            }
        }
    }

    private void executeTask(EventTriggerTaskEntity task) {
        long startTime = System.currentTimeMillis();
        String sourceType = task.getSourceType();
        try {
            if (ResourceType.APPLICATION.equals(sourceType)) {
                executeApplicationTask(task, startTime);
            } else if (ResourceType.TOOL.equals(sourceType)) {
                executeToolTask(task, startTime);
            } else {
                log.warn("Unknown source type: {}", sourceType);
            }
        } catch (Exception e) {
            float runTime = (System.currentTimeMillis() - startTime) / 1000f;
            saveRecord(task.getTriggerId(), task.getId(), sourceType, task.getSourceId(),
                    TaskState.FAILURE, runTime, buildErrorMeta(e));
            throw e;
        }
    }

    private void executeApplicationTask(EventTriggerTaskEntity task, long startTime) {
        String appId = task.getSourceId();
        JSONObject parameter = task.getParameter();
        String question = parameter != null ? parameter.getString("question") : null;
        if (StringUtils.isNotBlank(question)) {
            try {
                String chatId = applicationChatService.chatOpen(appId, true);
                Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
                ChatParams chatParams = ChatParams.builder()
                        .message(question)
                        .chatId(chatId)
                        .appId(appId)
                        .debug(true)
                        .reChat(false)
                        .stream(false)
                        .build();
                CompletableFuture<ChatResponse> future = applicationChatService.chatMessageAsync(chatParams, sink);
                ChatResponse response = future.join();
                float runTime = (System.currentTimeMillis() - startTime) / 1000f;
                TaskState state = (response != null && response.getAnswerTextList() != null) ? TaskState.SUCCESS : TaskState.FAILURE;
                JSONObject meta = (response != null) ? response.getRunDetails() : new JSONObject();
                saveRecord(task.getTriggerId(), task.getId(), ResourceType.APPLICATION, appId, state, runTime, meta);
                log.info("Application task executed: appId={}, chatId={}, state={}, runTime={}s",
                        appId, chatId, state, runTime);
            } catch (Exception e) {
                log.error("Failed to execute application task: appId={}, error={}", appId, e.getMessage(), e);
                float runTime = (System.currentTimeMillis() - startTime) / 1000f;
                saveRecord(task.getTriggerId(), task.getId(), ResourceType.APPLICATION, appId, TaskState.FAILURE, runTime, buildErrorMeta(e));
            }
        }
    }

    private void executeToolTask(EventTriggerTaskEntity task, long startTime) {
        String toolId = task.getSourceId();
        try {
            ToolEntity tool = toolService.lambdaQuery().select(ToolEntity::getCode, ToolEntity::getInitParams).eq(ToolEntity::getId, toolId).one();
            GroovyScriptExecutor scriptExecutor = new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
            JSONObject parameter = task.getParameter();
            Object response = scriptExecutor.execute(parameter);
            float runTime = (System.currentTimeMillis() - startTime) / 1000f;
            TaskState state = (response != null) ? TaskState.SUCCESS : TaskState.FAILURE;
            JSONObject meta = new JSONObject();
            int status = response != null ? 200 : 500;
            meta.put("tool_call", Map.of("index", 1, "type", "tool-node", "status", status, "params", parameter, "result", response != null ? response : ""));
            saveRecord(task.getTriggerId(), task.getId(), ResourceType.TOOL, toolId, state, runTime, meta);
            log.info("Tool task executed: toolId={}, state={}, runTime={}s", toolId, state, runTime);
        } catch (Exception e) {
            log.error("Failed to execute tool task: toolId={}, error={}", toolId, e.getMessage(), e);
            float runTime = (System.currentTimeMillis() - startTime) / 1000f;
            saveRecord(task.getTriggerId(), task.getId(), ResourceType.TOOL, toolId, TaskState.FAILURE, runTime, buildErrorMeta(e));
        }
    }

    private void saveRecord(String triggerId, String taskId, String sourceType, String sourceId,
                            TaskState state, float runTime, JSONObject meta) {
        try {
            EventTriggerTaskRecordEntity record = new EventTriggerTaskRecordEntity();
            record.setTriggerId(triggerId);
            record.setTriggerTaskId(taskId);
            record.setSourceType(sourceType);
            record.setSourceId(sourceId);
            record.setState(state.name());
            record.setRunTime(runTime);
            record.setTaskRecordId(record.getId());
            record.setMeta(meta);
            eventTriggerTaskRecordService.save(record);
        } catch (Exception e) {
            log.error("Failed to save task record: {}", e.getMessage(), e);
        }
    }

    private JSONObject buildErrorMeta(Exception e) {
        JSONObject meta = new JSONObject();
        meta.put("error", e.getMessage());
        meta.put("errorType", e.getClass().getSimpleName());
        return meta;
    }
}