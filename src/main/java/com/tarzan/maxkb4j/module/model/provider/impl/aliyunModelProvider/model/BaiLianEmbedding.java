package com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;

public class BaiLianEmbedding implements BaseModel {
    @Override
    public <T> T newInstance(String modelName, JSONObject credential) {
        QwenEmbeddingModel model=new QwenEmbeddingModel(null, credential.getString("dashscope_api_key"), modelName);
        return (T) model;
    }
}
