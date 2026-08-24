package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;


import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * XunFei (iFLYTEK) Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.XUN_FEI, name = "讯飞星火", icon = Provider.ICON_XUN_FEI)
public class XunFeiModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.XUN_FEI;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.XUNFEI_ULTRA_4_0, "", ModelType.LLM),
            new ModelInfo(ModelName.XUNFEI_MAX_32K, "", ModelType.LLM),
            new ModelInfo(ModelName.XUNFEI_GENERAL_V3_5, "", ModelType.LLM),
            new ModelInfo(ModelName.XUNFEI_GENERAL_V3, "", ModelType.LLM),
            new ModelInfo(ModelName.XUNFEI_LITE, "", ModelType.LLM)
    );

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }
}
