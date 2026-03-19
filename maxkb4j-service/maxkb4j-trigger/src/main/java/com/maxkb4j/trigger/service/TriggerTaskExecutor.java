package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatResponse;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.entity.EventTriggerTaskRecordEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerTaskExecutor {

    private final IEventTriggerTaskService eventTriggerTaskService;
    private final IEventTriggerTaskRecordService eventTriggerTaskRecordService;
    private final IApplicationChatService applicationChatService;

    /**
     * 执行触发器的所有关联任务
     */
    public void executeTriggerTasks(String triggerId) {
        if (StringUtils.isBlank(triggerId)) {
            return;
        }
        LambdaQueryWrapper<EventTriggerTaskEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(EventTriggerTaskEntity::getTriggerId, triggerId);
        wrapper.eq(EventTriggerTaskEntity::getIsActive, true);
        List<EventTriggerTaskEntity> tasks = eventTriggerTaskService.list(wrapper);

        if (tasks == null || tasks.isEmpty()) {
            log.info("No active tasks found for trigger: {}", triggerId);
            return;
        }

        for (EventTriggerTaskEntity task : tasks) {
            try {
                executeTask(task);
            } catch (Exception e) {
                log.error("Error executing task {} for trigger {}: {}",
                        task.getId(), triggerId, e.getMessage(), e);
                saveRecord(triggerId, task.getId(), task.getSourceType(), task.getSourceId(), "failed", 0f, null);
            }
        }
    }

    private void executeTask(EventTriggerTaskEntity task) {
        long startTime = System.currentTimeMillis();
        String sourceType = task.getSourceType();

        try {
            if ("APPLICATION".equals(sourceType)) {
                executeApplicationTask(task, startTime);
            } else if ("TOOL".equals(sourceType)) {
                executeToolTask(task, startTime);
            } else {
                log.warn("Unknown source type: {}", sourceType);
            }
        } catch (Exception e) {
            float runTime = (System.currentTimeMillis() - startTime) / 1000f;
            saveRecord(task.getTriggerId(), task.getId(), sourceType, task.getSourceId(),
                    "failed", runTime, buildErrorMeta(e));
            throw e;
        }
    }

    private void executeApplicationTask(EventTriggerTaskEntity task, long startTime) {
        String appId = task.getSourceId();
        JSONObject parameter = task.getParameter();

        String message = null;
        if (parameter != null) {
            message = parameter.getString("message");
        }
        if (StringUtils.isBlank(message)) {
            message = "定时触发";
        }

        try {
            String chatId = applicationChatService.chatOpen(appId, true);

            Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();

            ChatParams chatParams = ChatParams.builder()
                    .message(message)
                    .chatId(chatId)
                    .appId(appId)
                    .debug(true)
                    .reChat(false)
                    .stream(false)
                    .build();

            CompletableFuture<ChatResponse> future = applicationChatService.chatMessageAsync(chatParams, sink);
            ChatResponse response = future.join();

            float runTime = (System.currentTimeMillis() - startTime) / 1000f;
            String state = (response != null && response.getAnswerTextList() != null) ? "success" : "failed";
            saveRecord(task.getTriggerId(), task.getId(), "APPLICATION", appId, state, runTime, null);

            log.info("Application task executed: appId={}, chatId={}, state={}, runTime={}s",
                    appId, chatId, state, runTime);

        } catch (Exception e) {
            log.error("Failed to execute application task: appId={}, error={}", appId, e.getMessage(), e);
            float runTime = (System.currentTimeMillis() - startTime) / 1000f;
            saveRecord(task.getTriggerId(), task.getId(), "APPLICATION", appId, "failed", runTime, buildErrorMeta(e));
        }
    }

    private void executeToolTask(EventTriggerTaskEntity task, long startTime) {
        String toolId = task.getSourceId();
        log.info("Tool task triggered: toolId={}", toolId);

        float runTime = (System.currentTimeMillis() - startTime) / 1000f;
        saveRecord(task.getTriggerId(), task.getId(), "TOOL", toolId, "success", runTime, null);
    }

    private void saveRecord(String triggerId, String taskId, String sourceType, String sourceId,
                            String state, float runTime, JSONObject meta) {
        try {
            EventTriggerTaskRecordEntity record = new EventTriggerTaskRecordEntity();
            record.setTriggerId(triggerId);
            record.setTriggerTaskId(taskId);
            record.setSourceType(sourceType);
            record.setSourceId(sourceId);
            record.setState(state);
            record.setRunTime(runTime);
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