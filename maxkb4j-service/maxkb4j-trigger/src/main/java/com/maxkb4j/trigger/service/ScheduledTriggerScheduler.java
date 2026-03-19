package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.enums.ScheduleType;
import com.maxkb4j.trigger.enums.TriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTriggerScheduler implements ApplicationRunner {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final IEventTriggerService eventTriggerService;
    private final TriggerTaskExecutor triggerTaskExecutor;
    private final NextRunTimeCalculator nextRunTimeCalculator;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading scheduled triggers on application startup...");
        loadAndScheduleAllTriggers();
    }

    public void loadAndScheduleAllTriggers() {
        LambdaQueryWrapper<EventTriggerEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(EventTriggerEntity::getTriggerType, TriggerType.SCHEDULED.name());
        wrapper.eq(EventTriggerEntity::getIsActive, true);
        List<EventTriggerEntity> triggers = eventTriggerService.list(wrapper);
        if (CollectionUtils.isEmpty(triggers)) {
            log.info("No active scheduled triggers found");
            return;
        }
        for (EventTriggerEntity trigger : triggers) {
            try {
                scheduleTrigger(trigger);
            } catch (Exception e) {
                log.error("Failed to schedule trigger {}: {}", trigger.getId(), e.getMessage(), e);
            }
        }
        log.info("Scheduled {} triggers", scheduledTasks.size());
    }

    public void scheduleTrigger(EventTriggerEntity trigger) {
        if (trigger == null || trigger.getIsActive() == null || !trigger.getIsActive()) {
            return;
        }
        String triggerId = trigger.getId();
        JSONObject triggerSetting = trigger.getTriggerSetting();
        if (triggerSetting == null) {
            log.warn("Trigger {} has no triggerSetting", triggerId);
            return;
        }
        String scheduleTypeStr = triggerSetting.getString("scheduleType");
        if (StringUtils.isBlank(scheduleTypeStr)) {
            log.warn("Trigger {} has no scheduleType", triggerId);
            return;
        }
        cancelSchedule(triggerId);
        ScheduleType scheduleType = ScheduleType.fromValue(scheduleTypeStr);
        switch (scheduleType) {
            case DAILY -> scheduleDaily(triggerId, triggerSetting);
            case WEEKLY -> scheduleWeekly(triggerId, triggerSetting);
            case MONTHLY -> scheduleMonthly(triggerId, triggerSetting);
            case INTERVAL -> scheduleInterval(triggerId, triggerSetting);
        }
        log.info("Scheduled trigger {} with type {}", triggerId, scheduleType);
    }

    private void scheduleDaily(String triggerId, JSONObject setting) {
        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            log.warn("Failed to calculate next run time for daily trigger {}", triggerId);
            return;
        }
        scheduleAtTime(triggerId, nextRunTime, true);
    }

    private void scheduleWeekly(String triggerId, JSONObject setting) {
        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            log.warn("Failed to calculate next run time for weekly trigger {}", triggerId);
            return;
        }
        scheduleAtTime(triggerId, nextRunTime, false);
    }

    private void scheduleMonthly(String triggerId, JSONObject setting) {
        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            log.warn("Failed to calculate next run time for monthly trigger {}", triggerId);
            return;
        }
        scheduleAtTime(triggerId, nextRunTime, false);
    }

    private void scheduleInterval(String triggerId, JSONObject setting) {
        Integer intervalValue = setting.getInteger("intervalValue");
        String intervalUnit = setting.getString("intervalUnit");

        if (intervalValue == null || intervalValue <= 0 || StringUtils.isBlank(intervalUnit)) {
            log.warn("Invalid interval configuration for trigger {}", triggerId);
            return;
        }

        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            nextRunTime = LocalDateTime.now().plus(intervalValue, getChronoUnit(intervalUnit));
        }

        scheduleAtFixedRate(triggerId, nextRunTime, intervalValue, intervalUnit);
    }

    private void scheduleAtTime(String triggerId, LocalDateTime runTime, boolean isDaily) {
        Instant instant = DateTimeUtil.toInstant(runTime);

        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            executeTrigger(triggerId);
            if (isDaily) {
                rescheduleDaily(triggerId);
            } else {
                rescheduleFromSetting(triggerId);
            }
        }, instant);

        scheduledTasks.put(triggerId, future);
        log.info("Scheduled trigger {} to run at {}", triggerId, runTime);
    }

    private void scheduleAtFixedRate(String triggerId, LocalDateTime startTime, int intervalValue, String intervalUnit) {
        Instant startInstant = DateTimeUtil.toInstant(startTime);
        long periodMillis = calculatePeriodMillis(intervalValue, intervalUnit);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> executeTrigger(triggerId),
                startInstant,
                java.time.Duration.ofMillis(periodMillis)
        );

        scheduledTasks.put(triggerId, future);
        log.info("Scheduled trigger {} with interval {} {}", triggerId, intervalValue, intervalUnit);
    }

    private void executeTrigger(String triggerId) {
        log.info("Executing trigger {}", triggerId);
        try {
            triggerTaskExecutor.executeTriggerTasks(triggerId);
        } catch (Exception e) {
            log.error("Error executing trigger {}: {}", triggerId, e.getMessage(), e);
        }
    }

    private void rescheduleDaily(String triggerId) {
        EventTriggerEntity trigger = eventTriggerService.getById(triggerId);
        if (trigger == null || !Boolean.TRUE.equals(trigger.getIsActive())) {
            cancelSchedule(triggerId);
            return;
        }
        scheduleDaily(triggerId, trigger.getTriggerSetting());
    }

    private void rescheduleFromSetting(String triggerId) {
        EventTriggerEntity trigger = eventTriggerService.getById(triggerId);
        if (trigger == null || !Boolean.TRUE.equals(trigger.getIsActive())) {
            cancelSchedule(triggerId);
            return;
        }
        scheduleTrigger(trigger);
    }

    public void cancelSchedule(String triggerId) {
        ScheduledFuture<?> future = scheduledTasks.remove(triggerId);
        if (future != null) {
            boolean cancelled = future.cancel(false);
            log.info("Cancelled schedule for trigger {}, cancelled={}, isCancelled={}, isDone={}",
                    triggerId, cancelled, future.isCancelled(), future.isDone());
        } else {
            log.warn("No scheduled task found for trigger {}, current scheduled tasks: {}",
                    triggerId, scheduledTasks.keySet());
        }
    }

    public void rescheduleTrigger(EventTriggerEntity trigger) {
        if (trigger == null) {
            return;
        }
        cancelSchedule(trigger.getId());
        if (Boolean.TRUE.equals(trigger.getIsActive())) {
            scheduleTrigger(trigger);
        }
    }

    public boolean isScheduled(String triggerId) {
        ScheduledFuture<?> future = scheduledTasks.get(triggerId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    public int getScheduledCount() {
        return scheduledTasks.size();
    }

    private long calculatePeriodMillis(int intervalValue, String intervalUnit) {
        return switch (intervalUnit.toLowerCase()) {
            case "minutes" -> intervalValue * 60L * 1000L;
            case "hours" -> intervalValue * 60L * 60L * 1000L;
            default -> throw new IllegalArgumentException("Invalid interval unit: " + intervalUnit);
        };
    }

    private ChronoUnit getChronoUnit(String intervalUnit) {
        return switch (intervalUnit.toLowerCase()) {
            case "minutes" -> ChronoUnit.MINUTES;
            case "hours" -> ChronoUnit.HOURS;
            default -> ChronoUnit.MINUTES;
        };
    }
}