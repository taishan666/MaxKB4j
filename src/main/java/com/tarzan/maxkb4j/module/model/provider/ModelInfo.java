package com.tarzan.maxkb4j.module.model.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    private String modelType;
    @JsonIgnore
    private BaseModelCredential modelCredential;
    @JsonIgnore
    private BaseModel modelClass;

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelCredential = new BaseModelCredential(false,true);
    }

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass, boolean needUrl,boolean needApiKey) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelCredential = new BaseModelCredential(needUrl,needApiKey);

    }

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass,BaseModelCredential modelCredential) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelCredential = modelCredential;
    }

}
