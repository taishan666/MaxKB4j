package com.tarzan.maxkb4j.module.model.provider.service.impl.zhipumodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.service.BaseModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class ZhiPuEmbedding implements BaseModel<EmbeddingModel> {
    @Override
    public EmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  ZhipuAiEmbeddingModel.builder()
                //.baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }
}
