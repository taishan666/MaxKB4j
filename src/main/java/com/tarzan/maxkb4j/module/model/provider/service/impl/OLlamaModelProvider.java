package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.custom.credential.impl.BaseModelCredential;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class OLlamaModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.OLlama.getProvider());
        info.setName(ModelProviderEnum.OLlama.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/ollama_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("qwen:7b","", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("llama3:8b","", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("deepseek-r1:8b","", ModelType.LLM,new LlmModelParams()));
        modelInfos.add(new ModelInfo("nomic-embed-text","", ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("llava:7b","", ModelType.VISION, new LlmModelParams()));
        modelInfos.add(new ModelInfo("llava:13b","", ModelType.VISION,new LlmModelParams()));
        return modelInfos;
    }

    @Override
    public BaseModelCredential getModelCredential() {
        return new BaseModelCredential(true,false);
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }


}
