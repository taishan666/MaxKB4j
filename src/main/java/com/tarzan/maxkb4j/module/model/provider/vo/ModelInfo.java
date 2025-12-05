package com.tarzan.maxkb4j.module.model.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    private ModelType modelType;
    @JsonIgnore
    private ModelParams modelParams;
    @JsonIgnore
    private Class<?> modelClass;

    public ModelInfo(String name, String desc, ModelType modelType) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
    }

    public ModelInfo(String name, String desc, ModelType modelType, ModelParams modelParams) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelParams = modelParams;
    }


    public ModelInfo(String name, String desc, ModelType modelType, Class<?> modelClass, ModelParams modelParams) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelParams = modelParams;
    }


}


