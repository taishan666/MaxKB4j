package com.tarzan.maxkb4j.module.model.provider.impl.wenxinmodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class QianfanEmbedding implements BaseModel<EmbeddingModel> {
    @Override
    public QianfanEmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  QianfanEmbeddingModel.builder()
                //.baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .build();
    }
}
