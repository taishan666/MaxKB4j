package com.maxkb4j.trigger.enums;

import lombok.Getter;

@Getter
public enum SourceType {
    APPLICATION("application"),
    TOOL("tool");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    public static SourceType fromValue(String text) {
        for (SourceType type : SourceType.values()) {
            if (type.value.equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
