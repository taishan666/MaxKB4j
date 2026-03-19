package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.trigger.enums.ScheduleType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 下次执行时间计算器
 */
@Component
public class NextRunTimeCalculator {

    public String calculate(JSONObject triggerSetting) {
        if (triggerSetting == null) {
            return null;
        }

        String scheduleType = triggerSetting.getString("scheduleType");
        if (scheduleType == null) {
            return null;
        }

        List<String> timeList = getStringList(triggerSetting.get("time"));
        if (CollectionUtils.isEmpty(timeList)) {
            return null;
        }

        String timeStr = timeList.get(0);
        String[] timeParts = timeStr.split(":");
        if (timeParts.length < 2) {
            return null;
        }

        try {
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            return switch (ScheduleType.fromValue(scheduleType)) {
                case DAILY -> DateTimeUtil.getNextDayAtTime(hour, minute, 0).toString();
                case WEEKLY -> calculateWeekly(triggerSetting, hour, minute);
                case MONTHLY -> calculateMonthly(triggerSetting, hour, minute);
                case INTERVAL -> calculateInterval(triggerSetting, hour, minute);
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String calculateWeekly(JSONObject setting, int hour, int minute) {
        List<String> days = getStringList(setting.get("days"));
        if (CollectionUtils.isEmpty(days)) return null;
        return DateTimeUtil.getSameDayNextWeek(Integer.parseInt(days.get(0)), hour, minute, 0).toString();
    }

    private String calculateMonthly(JSONObject setting, int hour, int minute) {
        List<String> days = getStringList(setting.get("days"));
        if (CollectionUtils.isEmpty(days)) return null;
        return DateTimeUtil.getSameDayNextMonth(Integer.parseInt(days.get(0)), hour, minute, 0).toString();
    }

    private String calculateInterval(JSONObject setting, int hour, int minute) {
        Object value = setting.get("intervalValue");
        String unit = setting.getString("intervalUnit");
        if (value == null || unit == null) return null;
        return DateTimeUtil.getSameDayNextInterval(value.toString(), unit, hour, minute, 0).toString();
    }

    private List<String> getStringList(Object obj) {
        if (!(obj instanceof List<?> list)) return List.of();
        return list.stream()
                .map(item -> item instanceof String s ? s : item != null ? item.toString() : null)
                .filter(Objects::nonNull)
                .toList();
    }
}