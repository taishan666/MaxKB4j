package com.maxkb4j.model.provider;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.vo.ModelInfo;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.Provider;

@Component
@ModelProviderType(provider = Provider.MIN_MAX, name = "MinMax", icon = Provider.ICON_MIN_MAX)
public class MinMaxModelProvider extends OpenAiModelProvider {

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.MIN_MAX;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.MIN_MAX;
    }
}
