package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiImageModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("model_zhipu_provider")
public class ZhiPuModelProvider extends IModelProvider {
    @Override
    public ModelProviderInfo getBaseInfo() {
        ModelProviderInfo info = new ModelProviderInfo();
        info.setProvider(ModelProviderEnum.ZhiPu.getProvider());
        info.setName(ModelProviderEnum.ZhiPu.getName());
        ClassLoader classLoader = AliYunBaiLianModelProvider.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("icon/zhipuai_icon.svg");
        String icon= IoUtil.readToString(inputStream);
        info.setIcon(icon);
        return info;
    }

    @Override
    public List<ModelInfo> getModelList() {
        List<ModelInfo> modelInfos = new ArrayList<>();
        modelInfos.add(new ModelInfo("glm-4","", ModelType.LLM, new LlmModelParams()));
        modelInfos.add(new ModelInfo("glm-4v","", ModelType.LLM,  new LlmModelParams()));
        modelInfos.add(new ModelInfo("glm-3-turbo","", ModelType.LLM,  new LlmModelParams()));
        modelInfos.add(new ModelInfo("text-embedding-v3","", ModelType.EMBEDDING));
        modelInfos.add(new ModelInfo("glm-4v-plus","", ModelType.VISION));
        modelInfos.add(new ModelInfo("glm-4v","", ModelType.VISION,  new LlmModelParams()));
        modelInfos.add(new ModelInfo("glm-4v-flash","", ModelType.VISION, new LlmModelParams()));
        modelInfos.add(new ModelInfo("cogview-3","", ModelType.TTI));
        modelInfos.add(new ModelInfo("cogview-3-plus","", ModelType.TTI));
        modelInfos.add(new ModelInfo("cogview-3-flash","", ModelType.TTI));
        return modelInfos;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return ZhipuAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return  ZhipuAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return  ZhipuAiImageModel.builder()
                .apiKey(credential.getApiKey())
                .model(modelName).build();
    }


}
