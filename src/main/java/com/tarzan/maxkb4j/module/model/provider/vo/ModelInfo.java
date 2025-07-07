package com.tarzan.maxkb4j.module.model.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import com.tarzan.maxkb4j.module.model.provider.BaseModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModelParams;
import com.tarzan.maxkb4j.module.model.provider.LlmModelParams;
import lombok.Data;

@Data
public class ModelInfo {

    private String name;
    private String desc;
    private String modelType;
    @JsonIgnore
    private BaseModelCredential modelCredential;
    @JsonIgnore
    private BaseModelParams modelParams;
    @JsonIgnore
    private BaseModel modelClass;

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelCredential = new BaseModelCredential(false,true);
        this.modelParams = new LlmModelParams();
    }

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass, boolean needUrl,boolean needApiKey) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelCredential = new BaseModelCredential(needUrl,needApiKey);

    }

    public ModelInfo(String name, String desc, String modelType, BaseModel modelClass, BaseModelParams modelParams) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.modelClass = modelClass;
        this.modelCredential = new BaseModelCredential(false,true);
        this.modelParams = modelParams;
    }

}
