package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.custom.model.SiliconFlowImageModel;
import com.maxkb4j.model.custom.model.SiliconFlowScoringModel;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Silicon Flow Model Provider - OpenAI compatible API
 */
@Component
@ModelProviderType(provider = "SiliconFlow", name = "Silicon Flow", icon = "silicon_flow_icon.svg")
public class SiliconFlowModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.siliconflow.cn/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("deepseek-ai/DeepSeek-V3.2", "", ModelType.LLM),
            new ModelInfo("Pro/moonshotai/Kimi-K2.5", "", ModelType.LLM),
            new ModelInfo("Qwen/Qwen3-VL-32B-Thinking", "", ModelType.LLM),
            new ModelInfo("Pro/zai-org/GLM-4.7", "", ModelType.LLM),
            new ModelInfo("Pro/MiniMaxAI/MiniMax-M2.1", "", ModelType.LLM),
            new ModelInfo("tencent/Hunyuan-MT-7B", "", ModelType.LLM),
            new ModelInfo("Qwen/Qwen3-Embedding-8B", "", ModelType.EMBEDDING),
            new ModelInfo("BAAI/bge-m3", "", ModelType.EMBEDDING),
            new ModelInfo("netease-youdao/bce-embedding-base_v1", "", ModelType.EMBEDDING),
            new ModelInfo("Qwen/Qwen3-Reranker-8B", "", ModelType.RERANKER),
            new ModelInfo("BAAI/bge-reranker-v2-m3", "", ModelType.RERANKER),
            new ModelInfo("netease-youdao/bce-reranker-base_v1", "", ModelType.RERANKER),
            new ModelInfo("Qwen/Qwen3-VL-32B-Thinking", "", ModelType.VISION),
            new ModelInfo("Qwen/Qwen3-Image", "", ModelType.TTI),
            new ModelInfo("Kwai-Kolors/Kolors", "", ModelType.TTI),
            new ModelInfo("TeleAI/TeleSpeechASR", "", ModelType.STT),
            new ModelInfo("FunAudioLLM/SenseVoiceSmall", "", ModelType.STT),
            new ModelInfo("FunAudioLLM/CosyVoice2-0.5B", "", ModelType.TTS)
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
