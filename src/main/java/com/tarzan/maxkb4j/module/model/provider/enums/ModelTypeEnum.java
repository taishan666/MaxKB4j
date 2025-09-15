package com.tarzan.maxkb4j.module.model.provider.enums;

import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
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

    final private String modelType;

    ModelTypeEnum(String modelType) {
        this.modelType = modelType;
    }

    public static List<KeyAndValueVO> getModelTypeList() {
        List<KeyAndValueVO> list = new ArrayList<>();
        for (ModelTypeEnum modelType : ModelTypeEnum.values()) {
            list.add(new KeyAndValueVO( modelType.getModelType(),modelType.name()));
        }
        return list;
    }

}
