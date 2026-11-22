package com.maxkb4j.trigger.service;

import com.maxkb4j.trigger.model.TriggerSetting;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.trigger.entity.EventTriggerEntity;

import com.maxkb4j.trigger.enums.TriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerScheduler implements ApplicationRunner {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final IEventTriggerService eventTriggerService;
    private final TriggerTaskExecutor triggerTaskExecutor;
    private final NextRunTimeCalculator nextRunTimeCalculator;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /** 重调度失败后的补偿重试次数上限 */
    private static final int RESCHEDULE_MAX_RETRIES = 3;

    /** 重调度补偿重试间隔，用于熬过 DB 抖动等瞬时故障 */
    private static final Duration RESCHEDULE_RETRY_DELAY = Duration.ofMinutes(5);


    @Override
    public void run(ApplicationArguments args) {
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
        log.info("Loading scheduled triggers on application startup...");
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
        TriggerSetting setting = TriggerSetting.from(trigger.getTriggerSetting());
        if (setting == null) {
            log.warn("Trigger {} has no triggerSetting", triggerId);
            return;
        }
        if (!setting.hasScheduleType()) {
            log.warn("Trigger {} has no scheduleType", triggerId);
            return;
        }
        if (isScheduled(triggerId)) {
            cancelSchedule(triggerId);
        }
        switch (setting.scheduleType()) {
            case DAILY -> scheduleDaily(triggerId, setting);
            case WEEKLY -> scheduleWeekly(triggerId, setting);
            case MONTHLY -> scheduleMonthly(triggerId, setting);
            case INTERVAL -> scheduleInterval(triggerId, setting);
        }
        log.info("Scheduled trigger {} with type {}", triggerId, setting.scheduleType());
    }

    private void scheduleDaily(String triggerId, TriggerSetting setting) {
        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            log.warn("Failed to calculate next run time for daily trigger {}", triggerId);
            return;
        }
        scheduleAtTime(triggerId, nextRunTime, true);
    }

    private void scheduleWeekly(String triggerId, TriggerSetting setting) {
        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            log.warn("Failed to calculate next run time for weekly trigger {}", triggerId);
            return;
        }
        scheduleAtTime(triggerId, nextRunTime, false);
    }

    private void scheduleMonthly(String triggerId, TriggerSetting setting) {
        LocalDateTime nextRunTime = nextRunTimeCalculator.calculate(setting);
        if (nextRunTime == null) {
            log.warn("Failed to calculate next run time for monthly trigger {}", triggerId);
            return;
        }
        scheduleAtTime(triggerId, nextRunTime, false);
    }

    private void scheduleInterval(String triggerId, TriggerSetting setting) {
        Integer intervalValue = setting.intervalValue();
        String intervalUnit = setting.intervalUnit();
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
            try {
                executeTrigger(triggerId);
            } finally {
                // executeTrigger 内部已捕获 Exception，finally 兜底极端情况（如 Error），
                // 确保重调度必定被尝试，daily/weekly 调度链不因单次异常而断链
                safeReschedule(triggerId, isDaily);
            }
        }, instant);

        replaceScheduledTask(triggerId, future);
        log.info("Scheduled trigger {} to run at {}", triggerId, runTime);
    }

    private void scheduleAtFixedRate(String triggerId, LocalDateTime startTime, int intervalValue, String intervalUnit) {
        Instant startInstant = DateTimeUtil.toInstant(startTime);
        long periodMillis = calculatePeriodMillis(intervalValue, intervalUnit);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> executeTrigger(triggerId),
                startInstant,
                Duration.ofMillis(periodMillis)
        );

        replaceScheduledTask(triggerId, future);
        log.info("Scheduled trigger {} with interval {} {}", triggerId, intervalValue, intervalUnit);
    }

    /**
     * 用新任务替换同 triggerId 的旧调度；若旧任务仍在等待触发则取消之，
     * 防止补偿重调度与人工编辑保存并发时残留两条并行调度链导致重复执行。
     */
    private void replaceScheduledTask(String triggerId, ScheduledFuture<?> future) {
        ScheduledFuture<?> old = scheduledTasks.put(triggerId, future);
        if (old != null && !old.isDone() && !old.isCancelled()) {
            old.cancel(false);
            log.info("Replaced pending schedule for trigger {}", triggerId);
        }
    }

    /**
     * 一次性任务触发后的重调度入口。重调度链路上的任何异常（getById 的 DB 抖动、
     * TriggerSetting.from 的非法 scheduleType、时间/天配置解析失败等）都在此捕获并进入有限次补偿重试，
     * 保证异常不会逃逸到 ScheduledExecutorService 导致 daily/weekly 触发器静默停摆。
     */
    private void safeReschedule(String triggerId, boolean isDaily) {
        try {
            doReschedule(triggerId, isDaily);
        } catch (Exception e) {
            log.error("Failed to reschedule trigger {}: {}", triggerId, e.getMessage(), e);
            scheduleRescheduleRetry(triggerId, isDaily, 1);
        }
    }

    private void scheduleRescheduleRetry(String triggerId, boolean isDaily, int attempt) {
        if (attempt > RESCHEDULE_MAX_RETRIES) {
            log.error("Reschedule of trigger {} failed after {} retries, giving up; "
                    + "the trigger will stay idle until it is edited again or the application is restarted",
                    triggerId, RESCHEDULE_MAX_RETRIES);
            return;
        }
        Instant retryAt = Instant.now().plus(RESCHEDULE_RETRY_DELAY);
        try {
            taskScheduler.schedule(() -> {
                try {
                    doReschedule(triggerId, isDaily);
                } catch (Exception e) {
                    log.error("Reschedule retry {}/{} failed for trigger {}: {}",
                            attempt, RESCHEDULE_MAX_RETRIES, triggerId, e.getMessage(), e);
                    scheduleRescheduleRetry(triggerId, isDaily, attempt + 1);
                }
            }, retryAt);
            log.warn("Scheduled reschedule retry {}/{} for trigger {} at {}",
                    attempt, RESCHEDULE_MAX_RETRIES, triggerId, retryAt);
        } catch (Exception e) {
            log.error("Failed to schedule reschedule retry for trigger {}: {}", triggerId, e.getMessage(), e);
        }
    }

    private void doReschedule(String triggerId, boolean isDaily) {
        if (isDaily) {
            rescheduleDaily(triggerId);
        } else {
            rescheduleFromSetting(triggerId);
        }
    }

    private void executeTrigger(String triggerId) {
        try {
            EventTriggerEntity trigger = eventTriggerService.getById(triggerId);
            if (!isActiveScheduledTrigger(trigger)) {
                cancelSchedule(triggerId);
                return;
            }
            log.info("Executing trigger {}", triggerId);
            triggerTaskExecutor.execute(triggerId);
        } catch (Exception e) {
            log.error("Error executing trigger {}: {}", triggerId, e.getMessage(), e);
        }
    }

    /**
     * 按 daily 重调度。可能抛出异常（getById 的 DB 抖动、TriggerSetting.from 的非法 scheduleType、
     * 时间/天配置解析失败），仅由 {@link #safeReschedule(String, boolean)} 调用并守护。
     */
    private void rescheduleDaily(String triggerId) {
        EventTriggerEntity trigger = eventTriggerService.getById(triggerId);
        if (!isActiveScheduledTrigger(trigger)) {
            cancelSchedule(triggerId);
            return;
        }
        scheduleDaily(triggerId, TriggerSetting.from(trigger.getTriggerSetting()));
    }

    /**
     * 按当前持久化配置重调度（尊重配置在两次触发之间被修改的情形）。
     * 可能抛出异常（同 {@link #rescheduleDaily(String)}，另含 interval 配置非法时的 IAE），
     * 仅由 {@link #safeReschedule(String, boolean)} 调用并守护。
     */
    private void rescheduleFromSetting(String triggerId) {
        EventTriggerEntity trigger = eventTriggerService.getById(triggerId);
        if (!isActiveScheduledTrigger(trigger)) {
            cancelSchedule(triggerId);
            return;
        }
        scheduleTrigger(trigger);
    }

    private boolean isActiveScheduledTrigger(EventTriggerEntity trigger) {
        return trigger != null
                && Boolean.TRUE.equals(trigger.getIsActive())
                && TriggerType.SCHEDULED.name().equals(trigger.getTriggerType());
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
        if (Boolean.TRUE.equals(trigger.getIsActive())) {
            scheduleTrigger(trigger);
        } else {
            cancelSchedule(trigger.getId());
        }
    }

    public boolean isScheduled(String triggerId) {
        ScheduledFuture<?> future = scheduledTasks.get(triggerId);
        return future != null && !future.isCancelled() && !future.isDone();
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
            default -> throw new IllegalArgumentException("Invalid interval unit: " + intervalUnit);
        };
    }
}