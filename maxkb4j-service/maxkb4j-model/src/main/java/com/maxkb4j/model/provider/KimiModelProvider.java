package com.maxkb4j.model.provider;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.vo.ModelInfo;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

/**
 * Kimi Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.KIMI, name = "Kimi", icon = Provider.ICON_KIMI)
public class KimiModelProvider extends OpenAiModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.KIMI;
    }

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.KIMI;
    }
}
