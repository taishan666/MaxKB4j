package com.maxkb4j.trigger.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.trigger.enums.ScheduleType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下次执行时间计算器
 */
@Component
public class NextRunTimeCalculator {


    public LocalDateTime calculate(JSONObject triggerSetting){
        if (triggerSetting == null) {
            return null;
        }
        String scheduleType = triggerSetting.getString("scheduleType");
        if (scheduleType == null) {
            return null;
        }
        if (ScheduleType.INTERVAL.getValue().equals(scheduleType)){
            return calculateInterval(triggerSetting);
        }else {
            List<String> timeList = triggerSetting.getObject("time", new TypeReference<List<String>>() {});
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
                    case DAILY -> DateTimeUtil.getNextDayAtTime(hour, minute, 0);
                    case WEEKLY -> calculateWeekly(triggerSetting, hour, minute);
                    case MONTHLY -> calculateMonthly(triggerSetting, hour, minute);
                    // 处理未匹配的情况或 null
                    default -> throw new IllegalArgumentException("Unsupported schedule type: " + scheduleType);
                };
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
    public String calculateStr(JSONObject triggerSetting) {
        LocalDateTime nextRunTime = calculate(triggerSetting);
        if (nextRunTime == null){
            return "";
        }else {
            return nextRunTime.toString();
        }
    }

    private LocalDateTime calculateWeekly(JSONObject setting, int hour, int minute) {
        List<String> days = setting.getObject("days", new TypeReference<List<String>>() {});
        if (CollectionUtils.isEmpty(days)) return null;
        return DateTimeUtil.getSameDayNextWeek(Integer.parseInt(days.get(0)), hour, minute, 0);
    }

    private LocalDateTime calculateMonthly(JSONObject setting, int hour, int minute) {
        List<String> days =setting.getObject("days", new TypeReference<List<String>>() {});
        if (CollectionUtils.isEmpty(days)) return null;
        return DateTimeUtil.getSameDayNextMonth(Integer.parseInt(days.get(0)), hour, minute, 0);
    }

    private LocalDateTime calculateInterval(JSONObject setting) {
        Object value = setting.get("intervalValue");
        String unit = setting.getString("intervalUnit");
        if (value == null || unit == null) return null;
        return DateTimeUtil.getSameDayNextInterval(value.toString(), unit, 0);
    }

}