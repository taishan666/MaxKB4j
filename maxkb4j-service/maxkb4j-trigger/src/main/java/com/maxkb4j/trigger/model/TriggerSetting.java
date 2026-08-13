package com.maxkb4j.trigger.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.maxkb4j.trigger.enums.ScheduleType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 触发器设置类型化对象（第 3 期弱类型治理）。
 * <p>模块入口处经 {@link #from(JSONObject)} 一次性完成 JSONObject → TriggerSetting 转换，
 * 内部逻辑不再通过魔法键取值。字段名常量为前端/持久化契约，勿改。</p>
 *
 * @param scheduleType  调度类型，null 表示未配置
 * @param times         执行时间列表（HH:mm）
 * @param days          周几/每月几号列表（字符串形式）
 * @param intervalValue 间隔数值
 * @param intervalUnit  间隔单位（minutes/hours）
 * @param token         webhook 鉴权令牌
 */
public record TriggerSetting(
        ScheduleType scheduleType,
        List<String> times,
        List<String> days,
        Integer intervalValue,
        String intervalUnit,
        String token) {

    public static final String FIELD_SCHEDULE_TYPE = "scheduleType";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_DAYS = "days";
    public static final String FIELD_INTERVAL_VALUE = "intervalValue";
    public static final String FIELD_INTERVAL_UNIT = "intervalUnit";
    public static final String FIELD_TOKEN = "token";

    /**
     * 从持久化 JSONObject 转换，入参为 null 时返回 null。
     * scheduleType 取值非法时抛 IllegalArgumentException（与历史行为一致）。
     */
    public static TriggerSetting from(JSONObject json) {
        if (json == null) {
            return null;
        }
        String scheduleTypeStr = json.getString(FIELD_SCHEDULE_TYPE);
        ScheduleType scheduleType = StringUtils.isBlank(scheduleTypeStr) ? null : ScheduleType.fromValue(scheduleTypeStr);
        return new TriggerSetting(
                scheduleType,
                json.getObject(FIELD_TIME, new TypeReference<List<String>>() {}),
                json.getObject(FIELD_DAYS, new TypeReference<List<String>>() {}),
                json.getInteger(FIELD_INTERVAL_VALUE),
                json.getString(FIELD_INTERVAL_UNIT),
                json.getString(FIELD_TOKEN));
    }

    public boolean hasScheduleType() {
        return scheduleType != null;
    }

    /** 第一个执行时间（HH:mm），无则返回 null。 */
    public String firstTime() {
        return times == null || times.isEmpty() ? null : times.get(0);
    }

    /** 第一个日期项（周几/几号），无则返回 null。 */
    public String firstDay() {
        return days == null || days.isEmpty() ? null : days.get(0);
    }
}