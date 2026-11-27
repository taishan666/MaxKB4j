package com.maxkb4j.model.provider;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.consts.ProviderModels;
import org.springframework.stereotype.Component;


import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Tencent HunYuan Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.TENCENT, name = "腾讯混元", icon = Provider.ICON_TENCENT)
public class TencentModelProvider extends OpenAiModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.TENCENT;
    }

    @Override
    public String getDefaultBaseUrl() {
        return BaseUrl.TENCENT;
    }
}
