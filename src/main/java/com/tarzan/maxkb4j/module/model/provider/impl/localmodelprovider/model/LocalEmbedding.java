package com.tarzan.maxkb4j.module.model.provider.impl.localmodelprovider.model;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.BaseModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;

public class LocalEmbedding implements BaseModel<EmbeddingModel> {

    @Override
    public AllMiniLmL6V2QuantizedEmbeddingModel build(String modelName, ModelCredential credential, JSONObject params) {
        return  new AllMiniLmL6V2QuantizedEmbeddingModel();
    }
}
