package com.maxkb4j.model.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型状态。
 *
 * <p>对应 {@code model.status} 数据库字段（字符串）。新增取值时务必保持
 * {@link #key} 与历史持久化值一致，避免破坏已有数据。
 */
@Getter
public enum ModelStatus {

    SUCCESS("SUCCESS", "可用"),
    ERROR("ERROR", "异常"),
    DOWNLOAD("DOWNLOAD", "下载中"),
    PAUSE_DOWNLOAD("PAUSE_DOWNLOAD", "暂停下载"),
    ;

    private final String key;
    private final String name;

    ModelStatus(String key, String name) {
        this.key = key;
        this.name = name;
    }

    private static final Map<String, ModelStatus> KEY_MAP = new HashMap<>();

    static {
        for (ModelStatus status : ModelStatus.values()) {
            KEY_MAP.put(status.getKey(), status);
        }
    }

    public static ModelStatus getByKey(String key) {
        return KEY_MAP.get(key);
    }
}
