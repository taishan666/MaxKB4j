package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

@Component
@ModelProviderType(provider = Provider.MIN_MAX, name = "MinMax", icon = Provider.ICON_MIN_MAX)
public class MinMaxModelProvider extends OpenAiModelProvider {


    private static final String BASE_URL = BaseUrl.MIN_MAX;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.MINI_MAX_M2_7, "", ModelType.LLM),
            new ModelInfo(ModelName.MINI_MAX_M2_7_HIGHSPEED, "", ModelType.LLM),
            new ModelInfo(ModelName.MINI_MAX_M2_5, "", ModelType.LLM),
            new ModelInfo(ModelName.MINI_MAX_M2_5_HIGHSPEED, "", ModelType.LLM)
    );

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }
}
