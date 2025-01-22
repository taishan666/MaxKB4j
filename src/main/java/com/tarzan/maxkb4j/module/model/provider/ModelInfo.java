package com.tarzan.maxkb4j.module.model.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    @JsonProperty("model_type")
    private String modelType;
    @JsonIgnore
    private BaseModelCredential modelCredential;
    @JsonIgnore
    private BaseModel modelClass;

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelCredential = null;
        this.modelClass = modelClass;
    }

    public ModelInfo(String name, String desc, String modelType,BaseModelCredential modelCredential, BaseModel modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelCredential = modelCredential;
        this.modelClass = modelClass;
    }

}
