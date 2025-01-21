package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public abstract class IModelProvider {

    public abstract ModelProvideInfo getModelProvideInfo();

    public abstract ModelInfoManage getModelInfoManage();

    public abstract List<ModelInfo> getModelList();

    <T> T getModel(String modelName, String modelType, JSONObject modelCredential){
        ModelInfo modelInfo = getModelInfoManage().getModelInfo(modelType, modelName);
        System.out.println("modelInfo="+modelInfo);
        return modelInfo.getModelClass().newInstance(modelName, modelCredential);
    }

}
