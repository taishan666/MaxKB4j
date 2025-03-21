package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;

import java.util.List;

public abstract class IModelProvider {

    public abstract ModelProvideInfo getModelProvideInfo();

    public ModelInfoManage getModelInfoManage() {
        return new ModelInfoManage(getModelList());
    }

    public abstract List<ModelInfo> getModelList();

    public BaseModelCredential getModelCredential( String modelType,String modelName){
        ModelInfoManage modelInfoManage=getModelInfoManage();
        ModelInfo modelInfo = modelInfoManage.getModelInfo(modelType,modelName);
        assert modelInfo != null;
        return modelInfo.getModelCredential();
    };

    <T> T build(String modelName, String modelType, ModelCredential modelCredential) {
        List<ModelInfo> modelList = getModelList();
        ModelInfo modelInfo = modelList.stream().filter(model -> model.getModelType().equals(modelType) && model.getName().equals(modelName)).findFirst().orElse(null);
        assert modelInfo != null;
        return modelInfo.getModelClass().build(modelName, modelCredential,null);
    }

    <T> T build(String modelName, String modelType, ModelCredential modelCredential, JSONObject params) {
        List<ModelInfo> modelList = getModelList();
        ModelInfo modelInfo = modelList.stream().filter(model -> model.getModelType().equals(modelType) && model.getName().equals(modelName)).findFirst().orElse(null);
        assert modelInfo != null;
        return modelInfo.getModelClass().build(modelName, modelCredential,params);
    }

}
