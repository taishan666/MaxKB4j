package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public abstract class IModelProvider {

    public abstract ModelProvideInfo getModelProvideInfo();

    public abstract ModelInfoManage getModelInfoManage();

    public abstract List<ModelInfo> getModelList();

    <T> T getModel(String modelName, String modelType, JSONObject modelCredential){
        List<ModelInfo> modelList=getModelList();
        ModelInfo modelInfo =  modelList.stream().filter(model -> model.getModelType().equals(modelType) && model.getName().equals(modelName)).findFirst().orElse(null);
        assert modelInfo != null;
        return modelInfo.getModelClass().newInstance(modelName, modelCredential);
    }

}
