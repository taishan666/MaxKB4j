package com.tarzan.maxkb4j.module.model.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.module.model.custom.params.ModelParams;
import com.tarzan.maxkb4j.module.model.custom.params.impl.NoModelParams;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    private String modelType;
    @JsonIgnore
    private ModelParams modelParams;
    @JsonIgnore
    private Class<?> modelClass;

    public ModelInfo(String name, String desc, String modelType) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = null;
        this.modelParams = new NoModelParams();
    }

    public ModelInfo(String name, String desc, String modelType, ModelParams modelParams) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = null;
        this.modelParams = modelParams;
    }

    public ModelInfo(String name, String desc, String modelType, Class<?> modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelParams = new NoModelParams();
    }



    public ModelInfo(String name, String desc, String modelType, Class<?> modelClass, ModelParams modelParams) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelParams = modelParams;
    }


}


