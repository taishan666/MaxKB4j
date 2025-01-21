package com.tarzan.maxkb4j.module.model.provider.enums;

import com.tarzan.maxkb4j.module.model.provider.KeyAndValueVO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum ModelTypeEnum {
    LLM("大语言模型"),
    EMBEDDING("向量模型"),
    STT("语音识别"),
    TTS("语音合成"),
    IMAGE("图片理解"),
    TTI("图片生成"),
    RERANKER("重排模型"),
    ;

    final private String name;

    ModelTypeEnum(String name) {
        this.name = name;
    }

    public static List<KeyAndValueVO> getModelTypeList() {
        List<KeyAndValueVO> list = new ArrayList<>();
        for (ModelTypeEnum modelType : ModelTypeEnum.values()) {
            list.add(new KeyAndValueVO(modelType.name(), modelType.getName()));
        }
        return list;
    }

    public static void main(String[] args) {
        for (KeyAndValueVO modelType : ModelTypeEnum.getModelTypeList()) {
            System.out.println(modelType);
        }
    }
}
