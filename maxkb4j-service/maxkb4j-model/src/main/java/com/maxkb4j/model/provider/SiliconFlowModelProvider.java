package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.integration.image.model.SiliconFlowImageModel;
import com.maxkb4j.model.integration.rerank.SiliconFlowScoringModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

/**
 * Silicon Flow Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.SILICON_FLOW, name = "Silicon Flow", icon = Provider.ICON_SILICON_FLOW)
public class SiliconFlowModelProvider extends OpenAiModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.SILICON_FLOW;
    }

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.SILICON_FLOW;
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return new SiliconFlowImageModel(modelName, credential, params);
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new SiliconFlowScoringModel(modelName, credential, params);
    }
}
