package com.maxkb4j.trigger.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.trigger.enums.ScheduleType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TriggerSetting 解析契约测试（第 3 期弱类型治理）。
 */
class TriggerSettingTest {

    @Test
    void from_nullJson_returnsNull() {
        assertThat(TriggerSetting.from(null)).isNull();
    }

    @Test
    void from_parsesAllFields() {
        JSONObject json = new JSONObject();
        json.put(TriggerSetting.FIELD_SCHEDULE_TYPE, "weekly");
        json.put(TriggerSetting.FIELD_TIME, List.of("08:30"));
        json.put(TriggerSetting.FIELD_DAYS, List.of("1", "3"));
        json.put(TriggerSetting.FIELD_INTERVAL_VALUE, 5);
        json.put(TriggerSetting.FIELD_INTERVAL_UNIT, "minutes");
        json.put(TriggerSetting.FIELD_TOKEN, "tk-123");

        TriggerSetting setting = TriggerSetting.from(json);

        assertThat(setting.scheduleType()).isEqualTo(ScheduleType.WEEKLY);
        assertThat(setting.times()).containsExactly("08:30");
        assertThat(setting.days()).containsExactly("1", "3");
        assertThat(setting.intervalValue()).isEqualTo(5);
        assertThat(setting.intervalUnit()).isEqualTo("minutes");
        assertThat(setting.token()).isEqualTo("tk-123");
        assertThat(setting.firstTime()).isEqualTo("08:30");
        assertThat(setting.firstDay()).isEqualTo("1");
    }

    @Test
    void from_blankScheduleType_treatedAsAbsent() {
        JSONObject json = new JSONObject();
        json.put(TriggerSetting.FIELD_SCHEDULE_TYPE, "");

        TriggerSetting setting = TriggerSetting.from(json);

        assertThat(setting.hasScheduleType()).isFalse();
    }

    @Test
    void from_invalidScheduleType_throws() {
        JSONObject json = new JSONObject();
        json.put(TriggerSetting.FIELD_SCHEDULE_TYPE, "hourly");

        assertThatThrownBy(() -> TriggerSetting.from(json))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void firstAccessors_emptyListsReturnNull() {
        TriggerSetting setting = new TriggerSetting(ScheduleType.DAILY, List.of(), List.of(), null, null, null);

        assertThat(setting.firstTime()).isNull();
        assertThat(setting.firstDay()).isNull();
    }
}