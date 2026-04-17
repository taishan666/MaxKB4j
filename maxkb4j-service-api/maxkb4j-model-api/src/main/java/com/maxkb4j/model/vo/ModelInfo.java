package com.maxkb4j.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.IModelParams;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    private ModelType modelType;
    @JsonIgnore
    private IModelParams modelParams;

    public ModelInfo(String name, String desc, ModelType modelType) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
    }

    public ModelInfo(String name, String desc, ModelType modelType, IModelParams modelParams) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelParams = modelParams;
    }


}


