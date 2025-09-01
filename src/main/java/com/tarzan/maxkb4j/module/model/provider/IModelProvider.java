package com.tarzan.maxkb4j.module.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;

import java.util.List;

public abstract class IModelProvider {

    public abstract ModelProvideInfo getModelProvideInfo();

    public ModelInfoManage getModelInfoManage() {
        return new ModelInfoManage(getModelList());
    }

    public abstract List<ModelInfo> getModelList();

    public BaseModelCredential getModelCredential() {
        return new BaseModelCredential(false,true);
    }


    private BaseModelParams getDefaultModelParams(String modelType) {
        return switch (modelType) {
            case "LLM", "EMBEDDING" -> new LlmModelParams();
            default -> new NoModelParams();
        };
    }

    public BaseModelParams getModelParams(String modelType, String modelName){
        ModelInfoManage modelInfoManage=getModelInfoManage();
        ModelInfo modelInfo = modelInfoManage.getModelInfo(modelType,modelName);
        if (modelInfo == null){
            return getDefaultModelParams(modelType);
        }
        return modelInfo.getModelParams();
    }

    public boolean isSupport(String modelType) {
        List<ModelInfo> modelInfos =getModelList();
        return modelInfos.stream().anyMatch(e -> e.getModelType().equals(modelType));
    }

    @SuppressWarnings("unchecked")
    <T> T build(String modelName, String modelType, ModelCredential modelCredential, JSONObject params) {
        List<ModelInfo> modelList = getModelList();
        ModelInfo modelInfo = modelList.stream().filter(model -> model.getModelType().equals(modelType) && model.getName().equals(modelName)).findFirst().orElse(null);
        if (modelInfo == null){
            //没有的话，取第一个model的构造器
            modelInfo = modelList.stream().filter(model -> model.getModelType().equals(modelType)).findFirst().orElse(null);
        }
        try {
            // 创建 BaseModel 实现类的实例
            assert modelInfo != null;
            BaseModel<T> instance = (BaseModel<T>) modelInfo.getModelClass().getDeclaredConstructor().newInstance();
            return instance.build(modelName, modelCredential,params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build model: " + modelName, e);
        }
    }

}
