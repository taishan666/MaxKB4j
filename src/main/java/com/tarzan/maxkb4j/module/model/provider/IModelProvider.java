package com.tarzan.maxkb4j.module.model.provider;

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

    <T> T getModel(String modelName, String modelType, ModelCredential modelCredential) {
        List<ModelInfo> modelList = getModelList();
        ModelInfo modelInfo = modelList.stream().filter(model -> model.getModelType().equals(modelType) && model.getName().equals(modelName)).findFirst().orElse(null);
        assert modelInfo != null;
        return modelInfo.getModelClass().newInstance(modelName, modelCredential);
    }

}
