package com.tarzan.maxkb4j.module.model.provider.enums;

import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum ModelType {


    LLM("LLM", "大语言模型"),
    EMBEDDING("EMBEDDING", "向量模型"),
    STT("STT", "语音识别"),
    TTS("TTS", "语音合成"),
    IMAGE_UNDERSTANDING("IMAGE", "图片理解"),
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

    public static List<KeyAndValueVO> getModelTypeList() {
        List<KeyAndValueVO> list = new ArrayList<>();
        for (ModelType modelType : ModelType.values()) {
            list.add(new KeyAndValueVO(modelType.getName(), modelType.getKey()));
        }
        return list;
    }

    public static ModelType getByKey(String key) {
        for (ModelType type : ModelType.values()) {
            if (type.getKey().equals(key)) {
                return type;
            }
        }
        return null;
    }

}
