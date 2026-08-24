package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * ZhiPu (GLM) Model Provider
 */
@Component
@ModelProviderType(provider = Provider.ZHI_PU, name = "智谱清言", icon = Provider.ICON_ZHI_PU)
public class ZhiPuModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.ZHI_PU;

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.GLM_5_1, "", ModelType.LLM),
            new ModelInfo(ModelName.GLM_5, "", ModelType.LLM),
            new ModelInfo(ModelName.GLM_4, "", ModelType.LLM),
            new ModelInfo(ModelName.GLM_4V, "", ModelType.LLM),
            new ModelInfo(ModelName.GLM_3_TURBO, "", ModelType.LLM),
            new ModelInfo(ModelName.EMBEDDING_3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.GLM_4V_PLUS, "", ModelType.VISION),
            new ModelInfo(ModelName.GLM_4V, "", ModelType.VISION),
            new ModelInfo(ModelName.GLM_4V_FLASH, "", ModelType.VISION),
            new ModelInfo(ModelName.GLM_IMAGE, "", ModelType.TTI),
            new ModelInfo(ModelName.COGVIEW_4, "", ModelType.TTI),
            new ModelInfo(ModelName.COGVIEW_3_FLASH, "", ModelType.TTI)
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
