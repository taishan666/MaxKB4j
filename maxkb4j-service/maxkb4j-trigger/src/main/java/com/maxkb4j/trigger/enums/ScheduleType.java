package com.maxkb4j.trigger.enums;

import lombok.Getter;

@Getter
public enum ScheduleType {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    INTERVAL("interval"); // 修正了拼写：INTERVA -> INTERVAL, "interva" -> "interval"

    // 可选：提供一个 getter 方法以便外部获取该值
    private final String value; // 建议加上 final，因为枚举值通常不可变

    // 必须定义这个构造函数来匹配上面的参数
    ScheduleType(String value) {
        this.value = value;
    }

    // 可选：提供一个静态方法通过字符串反查枚举
    public static ScheduleType fromValue(String text) {
        for (ScheduleType type : ScheduleType.values()) {
            if (type.value.equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
