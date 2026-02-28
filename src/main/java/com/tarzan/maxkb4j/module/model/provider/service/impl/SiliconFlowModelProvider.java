package com.tarzan.maxkb4j.module.model.provider.service.impl;

import com.tarzan.maxkb4j.module.model.custom.params.impl.LlmModelParams;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;

import java.util.List;

/**
 * Silicon Flow Model Provider - OpenAI compatible API
 */
public class SiliconFlowModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = "https://api.siliconflow.cn/v1";
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo("deepseek-ai/DeepSeek-V3.2", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("Pro/moonshotai/Kimi-K2.5", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("Qwen/Qwen3-VL-32B-Thinking", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("Pro/zai-org/GLM-4.7", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("Pro/MiniMaxAI/MiniMax-M2.1", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("tencent/Hunyuan-MT-7B", "", ModelType.LLM, new LlmModelParams()),
            new ModelInfo("Qwen/Qwen3-Embedding-8B", "", ModelType.EMBEDDING),
            new ModelInfo("BAAI/bge-m3", "", ModelType.EMBEDDING),
            new ModelInfo("netease-youdao/bce-embedding-base_v1", "", ModelType.EMBEDDING),
            new ModelInfo("Qwen/Qwen3-Reranker-8B", "", ModelType.RERANKER),
            new ModelInfo("BAAI/bge-reranker-v2-m3", "", ModelType.RERANKER),
            new ModelInfo("netease-youdao/bce-reranker-base_v1", "", ModelType.RERANKER),
            new ModelInfo("Qwen/Qwen3-VL-32B-Thinking", "", ModelType.VISION, new LlmModelParams()),
            new ModelInfo("Qwen/Qwen3-Image", "", ModelType.TTI)
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
