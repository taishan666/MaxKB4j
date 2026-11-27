package com.maxkb4j.model.provider;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.vo.ModelInfo;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

/**
 * XunFei (iFLYTEK) Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.XUN_FEI, name = "讯飞星火", icon = Provider.ICON_XUN_FEI)
public class XunFeiModelProvider extends OpenAiModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.XUN_FEI;
    }

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.XUN_FEI;
    }
}
