package com.tarzan.maxkb4j.module.model.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    @JsonProperty("model_type")
    private String modelType;
    private MaxKBBaseModel modelClass;

    public ModelInfo(String name, String desc, String modelType, MaxKBBaseModel modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
    }

}
