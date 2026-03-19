package com.maxkb4j.trigger.enums;

import lombok.Getter;

@Getter
public enum TriggerType {
    SCHEDULED("scheduled");

    private final String value;

    TriggerType(String value) {
        this.value = value;
    }

    public static TriggerType fromValue(String text) {
        for (TriggerType type : TriggerType.values()) {
            if (type.value.equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
