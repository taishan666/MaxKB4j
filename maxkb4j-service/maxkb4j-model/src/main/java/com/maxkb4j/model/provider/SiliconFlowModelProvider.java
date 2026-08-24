package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.custom.model.SiliconFlowImageModel;
import com.maxkb4j.model.custom.model.SiliconFlowScoringModel;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.stereotype.Component;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Silicon Flow Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = Provider.SILICON_FLOW, name = "Silicon Flow", icon = Provider.ICON_SILICON_FLOW)
public class SiliconFlowModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.SILICON_FLOW;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.DEEPSEEK_AI_V3_2, "", ModelType.LLM),
            new ModelInfo(ModelName.PRO_KIMI_K2_5, "", ModelType.LLM),
            new ModelInfo(ModelName.QWEN3_VL_32B_THINKING, "", ModelType.LLM),
            new ModelInfo(ModelName.PRO_GLM_4_7, "", ModelType.LLM),
            new ModelInfo(ModelName.PRO_MINI_MAX_M2_1, "", ModelType.LLM),
            new ModelInfo(ModelName.HUNYUAN_MT_7B, "", ModelType.LLM),
            new ModelInfo(ModelName.QWEN3_EMBEDDING_8B, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.BAAI_BGE_M3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.BCE_EMBEDDING_BASE_V1, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.QWEN3_RERANKER_8B, "", ModelType.RERANKER),
            new ModelInfo(ModelName.BAAI_BGE_RERANKER_V2_M3, "", ModelType.RERANKER),
            new ModelInfo(ModelName.BCE_RERANKER_BASE_V1, "", ModelType.RERANKER),
            new ModelInfo(ModelName.QWEN3_VL_32B_THINKING, "", ModelType.VISION),
            new ModelInfo(ModelName.QWEN3_IMAGE, "", ModelType.TTI),
            new ModelInfo(ModelName.KOLORS, "", ModelType.TTI),
            new ModelInfo(ModelName.TELE_SPEECH_ASR, "", ModelType.STT),
            new ModelInfo(ModelName.SENSE_VOICE_SMALL, "", ModelType.STT),
            new ModelInfo(ModelName.COSY_VOICE_2_0_5B, "", ModelType.TTS)
    );

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return new SiliconFlowImageModel(modelName, credential, params);
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new SiliconFlowScoringModel(modelName, credential, params);
    }
}
