package com.maxkb4j.model.provider;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

public class MinMaxModelProvider extends OpenAiModelProvider {


    private static final String BASE_URL = "https://api.minimaxi.com/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("MiniMax-M2.7", "", ModelType.LLM),
            new ModelInfo("MiniMax-M2.7-highspeed", "", ModelType.LLM),
            new ModelInfo("MiniMax-M2.5", "", ModelType.LLM),
            new ModelInfo("MiniMax-M2.5-highspeed", "", ModelType.LLM)
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
