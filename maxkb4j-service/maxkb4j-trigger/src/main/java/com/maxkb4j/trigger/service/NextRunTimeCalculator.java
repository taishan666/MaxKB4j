package com.maxkb4j.trigger.service;

import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.trigger.enums.ScheduleType;
import com.maxkb4j.trigger.model.TriggerSetting;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 下次执行时间计算器（第 3 期：入参类型化为 {@link TriggerSetting}）。
 */
@Component
public class NextRunTimeCalculator {

    public LocalDateTime calculate(TriggerSetting setting) {
        if (setting == null || setting.scheduleType() == null) {
            return null;
        }
        if (setting.scheduleType() == ScheduleType.INTERVAL) {
            return calculateInterval(setting);
        }
        String timeStr = setting.firstTime();
        if (timeStr == null) {
            return null;
        }
        String[] timeParts = timeStr.split(":");
        if (timeParts.length < 2) {
            return null;
        }
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        return switch (setting.scheduleType()) {
            case DAILY -> DateTimeUtil.getNextDayAtTime(hour, minute, 0);
            case WEEKLY -> calculateWeekly(setting, hour, minute);
            case MONTHLY -> calculateMonthly(setting, hour, minute);
            case INTERVAL -> throw new IllegalStateException("INTERVAL already handled above");
        };
    }

    public String calculateStr(TriggerSetting setting) {
        LocalDateTime nextRunTime = calculate(setting);
        return nextRunTime == null ? "" : nextRunTime.toString();
    }

    private LocalDateTime calculateWeekly(TriggerSetting setting, int hour, int minute) {
        String day = setting.firstDay();
        if (day == null) {
            return null;
        }
        return DateTimeUtil.getSameDayNextWeek(Integer.parseInt(day), hour, minute, 0);
    }

    private LocalDateTime calculateMonthly(TriggerSetting setting, int hour, int minute) {
        String day = setting.firstDay();
        if (day == null) {
            return null;
        }
        return DateTimeUtil.getSameDayNextMonth(Integer.parseInt(day), hour, minute, 0);
    }

    private LocalDateTime calculateInterval(TriggerSetting setting) {
        Integer value = setting.intervalValue();
        String unit = setting.intervalUnit();
        if (value == null || unit == null) {
            return null;
        }
        return DateTimeUtil.getSameDayNextInterval(value.toString(), unit, 0);
    }
}