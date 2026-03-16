package com.maxkb4j.model.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum ModelType {


    LLM("LLM", "大语言模型"),
    EMBEDDING("EMBEDDING", "向量模型"),
    STT("STT", "语音识别"),
    TTS("TTS", "语音合成"),
    VISION("VISION", "图片理解"),
    TTI("TTI", "图片生成"),
    RERANKER("RERANKER", "重排模型"),
    ;



    @Getter
    private final String key;
    private final String name;

    ModelType(String key, String name) {
        this.key = key;
        this.name = name;
    }

    private static final Map<String, ModelType> KEY_MAP = new HashMap<>();

    static {
        for (ModelType type : ModelType.values()) {
            KEY_MAP.put(type.getKey(), type);
        }
    }

    public static List<ModelType> getModelTypeList() {
        List<ModelType> list = new ArrayList<>();
        Collections.addAll(list, ModelType.values());
        return list;
    }

    public static ModelType getByKey(String key) {
        return KEY_MAP.get(key);
    }

}
