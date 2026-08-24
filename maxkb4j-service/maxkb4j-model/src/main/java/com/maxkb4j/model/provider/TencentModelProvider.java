package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
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

    private static final String BASE_URL = BaseUrl.TENCENT;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.HY3, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_PRO, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_STANDARD, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_LITE, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_ROLE, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_FUNCTIONCALL, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_CODE, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_EMBEDDING, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.HUNYUAN_VISION, "", ModelType.VISION),
            new ModelInfo(ModelName.HUNYUAN_DIT, "", ModelType.TTI)
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
