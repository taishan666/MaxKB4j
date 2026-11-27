package com.maxkb4j.model.provider;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.vo.ModelInfo;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

/**
 * ZhiPu (GLM) Model Provider
 */
@Component
@ModelProviderType(provider = Provider.ZHI_PU, name = "智谱清言", icon = Provider.ICON_ZHI_PU)
public class ZhiPuModelProvider extends OpenAiModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.ZHI_PU;
    }

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.ZHI_PU;
    }
}
