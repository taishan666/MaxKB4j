package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;


import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Kimi Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.KIMI, name = "Kimi", icon = Provider.ICON_KIMI)
public class KimiModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.KIMI;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.KIMI_K2_6, "", ModelType.LLM),
            new ModelInfo(ModelName.KIMI_K2_5, "", ModelType.LLM),
            new ModelInfo(ModelName.KIMI_K2_THINKING, "", ModelType.LLM),
            new ModelInfo(ModelName.KIMI_K2_THINKING_TURBO, "", ModelType.LLM),
            new ModelInfo(ModelName.MOONSHOT_V1_8K, "", ModelType.LLM),
            new ModelInfo(ModelName.MOONSHOT_V1_32K, "", ModelType.LLM),
            new ModelInfo(ModelName.MOONSHOT_V1_128K, "", ModelType.LLM),
            new ModelInfo(ModelName.KIMI_K2_6, "",ModelType.VISION),
            new ModelInfo(ModelName.KIMI_K2_5, "", ModelType.VISION)
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
