package com.maxkb4j.model.consts;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.integration.audio.*;
import com.maxkb4j.model.integration.image.QwenImageModelParams;
import com.maxkb4j.model.integration.image.WanXImageModelParams;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.WanxModelName;

import java.util.List;

public interface ProviderModels {

    List<ModelInfo> ALI_YUN_BAI_LIAN = List.of(
            new ModelInfo(ModelConstants.ModelName.QWEN_3_7_PLUS, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.QWEN_3_6_PLUS, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.QWEN_3_5_PLUS, "", ModelType.LLM),
            new ModelInfo(QwenModelName.QWEN_PLUS, "", ModelType.LLM),
            new ModelInfo(QwenModelName.QWEN_MAX, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.TEXT_EMBEDDING_V3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.TEXT_EMBEDDING_V4, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.PARAFORMER_REALTIME_V2, "", ModelType.STT),
            new ModelInfo(ModelConstants.ModelName.FUN_ASR_REALTIME, "", ModelType.STT),
            new ModelInfo(ModelConstants.ModelName.GUMMY_REALTIME_V1, "", ModelType.STT, new GummySTTParams()),
            new ModelInfo(ModelConstants.ModelName.COSYVOICE_V1, "", ModelType.TTS, new CosyVoiceV1TTSParams()),
            new ModelInfo(ModelConstants.ModelName.COSYVOICE_V2, "", ModelType.TTS, new CosyVoiceV2TTSParams()),
            new ModelInfo(ModelConstants.ModelName.SAMBERT_V1, "", ModelType.TTS, new SamBertTTSParams()),
            new ModelInfo(ModelConstants.ModelName.QWEN3_TTS_FLASH, "", ModelType.TTS, new QwenTTSParams()),
            new ModelInfo(ModelConstants.ModelName.QWEN_TTS, "", ModelType.TTS, new QwenTTSParams()),
            new ModelInfo(ModelConstants.ModelName.QWEN_3_6_PLUS, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.QWEN_3_5_PLUS, "", ModelType.VISION),
            new ModelInfo(QwenModelName.QWEN_VL_PLUS, "", ModelType.VISION),
            new ModelInfo(QwenModelName.QWEN_VL_MAX, "", ModelType.VISION),
            new ModelInfo(WanxModelName.WANX2_1_T2I_TURBO, "", ModelType.TTI, new WanXImageModelParams()),
            new ModelInfo(WanxModelName.WANX2_1_T2I_PLUS, "", ModelType.TTI, new WanXImageModelParams()),
            new ModelInfo(ModelConstants.ModelName.QWEN_IMAGE_PLUS, "", ModelType.TTI, new QwenImageModelParams()),
            new ModelInfo(ModelConstants.ModelName.GTE_RERANK, "", ModelType.RERANKER),
            new ModelInfo(ModelConstants.ModelName.QWEN3_RERANK, "", ModelType.RERANKER)
    );

    List<ModelInfo> ANTHROPIC = List.of(
            new ModelInfo(ModelConstants.ModelName.CLAUDE_3_OPUS, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.CLAUDE_3_SONNET, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.CLAUDE_3_HAIKU, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.CLAUDE_3_5_SONNET, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.CLAUDE_3_5_HAIKU, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.CLAUDE_3_5_SONNET, "", ModelType.VISION)
    );

    List<ModelInfo> AZURE = List.of(
            new ModelInfo("Azure OpenAI", "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4O, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4O_MINI, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.TEXT_EMBEDDING_3_LARGE, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.TEXT_EMBEDDING_3_SMALL, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.TEXT_EMBEDDING_ADA_002, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.GPT_4O, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.GPT_4O_MINI, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.DALLE_3, "", ModelType.TTI)
    );

    List<ModelInfo> DEEP_SEEK = List.of(
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_V4_FLASH_VISION, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_V4_FLASH_VISION, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_V4_FLASH, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_V4_PRO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_CHAT, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_REASONER, "", ModelType.LLM)
    );

    List<ModelInfo> GEMINI = List.of(
            new ModelInfo(ModelConstants.ModelName.GEMINI_1_0_PRO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GEMINI_1_0_PRO_VISIO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GEMINI_EMBEDDING_001, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.GEMINI_1_5_FLASH, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.GEMINI_1_5_PRO, "", ModelType.VISION)
    );

    List<ModelInfo> KIMI = List.of(
            new ModelInfo(ModelConstants.ModelName.KIMI_K2_6, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.KIMI_K2_5, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.KIMI_K2_THINKING, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.KIMI_K2_THINKING_TURBO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.MOONSHOT_V1_8K, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.MOONSHOT_V1_32K, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.MOONSHOT_V1_128K, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.KIMI_K2_6, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.KIMI_K2_5, "", ModelType.VISION)
    );

    List<ModelInfo> MIN_MAX = List.of(
            new ModelInfo(ModelConstants.ModelName.MINI_MAX_M2_7, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.MINI_MAX_M2_7_HIGHSPEED, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.MINI_MAX_M2_5, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.MINI_MAX_M2_5_HIGHSPEED, "", ModelType.LLM)
    );

    List<ModelInfo> OLLAMA = List.of(
            new ModelInfo(ModelConstants.ModelName.QWEN_7B, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.LLAMA3_8B, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_R1_8B, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.NOMIC_EMBED_TEXT, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.LLAVA_7B, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.LLAVA_13B, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.X_Z_IMAGE_TURBO, "", ModelType.TTI)
    );

    List<ModelInfo> OPENAI = List.of(
            new ModelInfo(ModelConstants.ModelName.GPT_3_5_TURBO, "GPT-3.5 Turbo", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4, "GPT-4", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4O, "GPT-4 Omni", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4O_MINI, "GPT-4 Omni Mini", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4_TURBO, "GPT-4 Turbo", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GPT_4_TURBO_PREVIEW, "GPT-4 Turbo Preview", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.TEXT_EMBEDDING_ADA_002, "Text Embedding Ada v2", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.WHISPER_1, "Whisper Speech-to-Text", ModelType.STT),
            new ModelInfo(ModelConstants.ModelName.TTS_1, "Text-to-Speech", ModelType.TTS),
            new ModelInfo(ModelConstants.ModelName.GPT_4O, "GPT-4 Vision", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.DALLE_2, "DALL·E 2", ModelType.TTI)
    );

    List<ModelInfo> SILICON_FLOW = List.of(
            new ModelInfo(ModelConstants.ModelName.DEEPSEEK_AI_V3_2, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.PRO_KIMI_K2_5, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.QWEN3_VL_32B_THINKING, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.PRO_GLM_4_7, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.PRO_MINI_MAX_M2_1, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_MT_7B, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.QWEN3_EMBEDDING_8B, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.BAAI_BGE_M3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.BCE_EMBEDDING_BASE_V1, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.QWEN3_RERANKER_8B, "", ModelType.RERANKER),
            new ModelInfo(ModelConstants.ModelName.BAAI_BGE_RERANKER_V2_M3, "", ModelType.RERANKER),
            new ModelInfo(ModelConstants.ModelName.BCE_RERANKER_BASE_V1, "", ModelType.RERANKER),
            new ModelInfo(ModelConstants.ModelName.QWEN3_VL_32B_THINKING, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.QWEN3_IMAGE, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.KOLORS, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.TELE_SPEECH_ASR, "", ModelType.STT),
            new ModelInfo(ModelConstants.ModelName.SENSE_VOICE_SMALL, "", ModelType.STT),
            new ModelInfo(ModelConstants.ModelName.COSY_VOICE_2_0_5B, "", ModelType.TTS)
    );

    List<ModelInfo> TENCENT = List.of(
            new ModelInfo(ModelConstants.ModelName.HY4, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HY3, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_PRO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_STANDARD, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_LITE, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_ROLE, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_FUNCTIONCALL, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_CODE, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_EMBEDDING, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_VISION, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.HUNYUAN_DIT, "", ModelType.TTI)
    );


    List<ModelInfo> VOLCANIC_ENGINE = List.of(
            new ModelInfo(ModelConstants.ModelName.DOUBAO_1_5_PRO_32K, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_SEED_1_6, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_SEED_1_6_FLASH, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_SEED_1_6_THINKING, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_SEED_1_6_VISION, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_SEEDREAM_4_0, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_SEEDREAM_4_5, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.DOUBAO_EMBEDDING_TEXT, "", ModelType.EMBEDDING)
    );

    List<ModelInfo> WEN_XIN = List.of(
            new ModelInfo(ModelConstants.ModelName.ERNIE_BOT_4, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.ERNIE_BOT, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.ERNIE_BOT_TURBO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.EMBEDDING_V1, "", ModelType.EMBEDDING)
    );

    List<ModelInfo> X_INFERENCE = List.of(
            new ModelInfo(ModelConstants.ModelName.QWEN3_8B, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.BGE_M3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.LLAVA_7B, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.SDXL_TURBO, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.BGE_RERANKER_BASE, "", ModelType.RERANKER),
            new ModelInfo(ModelConstants.ModelName.CHAT_TTS, "", ModelType.TTS),
            new ModelInfo(ModelConstants.ModelName.WHISPER_LARGE_V3, "", ModelType.STT)
    );

    List<ModelInfo> XUN_FEI = List.of(
            new ModelInfo(ModelConstants.ModelName.XUNFEI_ULTRA_4_0, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.XUNFEI_MAX_32K, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.XUNFEI_GENERAL_V3_5, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.XUNFEI_GENERAL_V3, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.XUNFEI_LITE, "", ModelType.LLM)
    );

    List<ModelInfo> ZHI_PU = List.of(
            new ModelInfo(ModelConstants.ModelName.GLM_5_3_FLASH, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_5_3, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_5_2, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_5_1, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_5, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_4, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_4V, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.GLM_3_TURBO, "", ModelType.LLM),
            new ModelInfo(ModelConstants.ModelName.EMBEDDING_3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelConstants.ModelName.GLM_4V_PLUS, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.GLM_4V, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.GLM_4V_FLASH, "", ModelType.VISION),
            new ModelInfo(ModelConstants.ModelName.GLM_IMAGE, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.COGVIEW_4, "", ModelType.TTI),
            new ModelInfo(ModelConstants.ModelName.COGVIEW_3_FLASH, "", ModelType.TTI)
    );

    List<ModelInfo> LOCAL = List.of();
}
