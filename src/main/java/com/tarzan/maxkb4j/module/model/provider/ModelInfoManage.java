package com.tarzan.maxkb4j.module.model.provider;

import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import lombok.Data;

import java.util.List;

@Data
public class ModelInfoManage {

    private List<ModelInfo> modelList;

    public ModelInfoManage(List<ModelInfo> modelList) {
        this.modelList = modelList;
    }

    public ModelInfo getModelInfo(String modelType,String modelName){
       return modelList.stream().filter(modelInfo -> modelInfo.getModelType().equals(modelType) && modelInfo.getName().equals(modelName)).findFirst().orElse(null);
    }

}
