package com.maxkb4j.model.consts;

/**
 * 模型模块常量定义
 * <p>
 * 集中管理模型提供方标识、默认服务地址、模型名称、参数键名、请求字段、HTTP 端点、
 * 控制器路由等字符串，避免魔法字符串散落在各提供方与模型实现中。
 *
 * @author tarzan
 */
public interface ModelConstants {

    /**
     * 模型提供方标识与图标
     */
    interface Provider {
        String OPENAI = "OpenAI";
        String ALI_YUN_BAI_LIAN = "AliYunBaiLian";
        String ANTHROPIC = "Anthropic";
        String AZURE = "Azure";
        String DEEP_SEEK = "DeepSeek";
        String GEMINI = "Gemini";
        String KIMI = "Kimi";
        String MIN_MAX = "MinMax";
        String OLLAMA = "OLlama";
        String SILICON_FLOW = "SiliconFlow";
        String TENCENT = "Tencent";
        String VOLCANIC_ENGINE = "VolcanicEngine";
        String WEN_XIN = "WenXin";
        String X_INFERENCE = "XInference";
        String XUN_FEI = "XunFei";
        String ZHI_PU = "ZhiPu";

        String ICON_OPENAI = "openai_icon.svg";
        String ICON_ALI_YUN_BAI_LIAN = "qwen_icon.svg";
        String ICON_ANTHROPIC = "anthropic_icon.svg";
        String ICON_AZURE = "azure_icon.svg";
        String ICON_DEEP_SEEK = "deepseek_icon.svg";
        String ICON_GEMINI = "gemini_icon.svg";
        String ICON_KIMI = "kimi_icon.svg";
        String ICON_MIN_MAX = "minmax_icon.svg";
        String ICON_OLLAMA = "ollama_icon.svg";
        String ICON_SILICON_FLOW = "silicon_flow_icon.svg";
        String ICON_TENCENT = "tencent_icon.svg";
        String ICON_VOLCANIC_ENGINE = "volcanic_engine_icon.svg";
        String ICON_WEN_XIN = "wenxin_icon.svg";
        String ICON_X_INFERENCE = "xinference_icon.svg";
        String ICON_XUN_FEI = "xf_icon.svg";
        String ICON_ZHI_PU = "zhipu_ai_icon.svg";
    }

    /**
     * 各提供方默认服务地址
     */
    interface BaseUrl {
        String OPENAI = "https://api.openai.com/v1";
        String ALI_YUN_BAI_LIAN = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        String ANTHROPIC = "https://api.anthropic.com";
        String DEEP_SEEK = "https://api.deepseek.com/v1";
        String KIMI = "https://api.moonshot.cn/v1";
        String LOCAL_AI = "http://host.docker.internal:8080";
        String MIN_MAX = "https://api.minimaxi.com/v1";
        String OLLAMA = "http://host.docker.internal:11434";
        String SILICON_FLOW = "https://api.siliconflow.cn/v1";
        String TENCENT = "https://api.hunyuan.cloud.tencent.com/v1";
        String VOLCANIC_ENGINE = "https://ark.cn-beijing.volces.com/api/v3";
        String X_INFERENCE = "http://host.docker.internal:9997";
        String XUN_FEI = "https://spark-api-open.xf-yun.com/v1/";
        String ZHI_PU = "https://open.bigmodel.cn/api/paas/v4";
    }

    /**
     * 模型名称
     */
    interface ModelName {
        String GPT_3_5_TURBO = "gpt-3.5-turbo";
        String GPT_4 = "gpt-4";
        String GPT_4O = "gpt-4o";
        String GPT_4O_MINI = "gpt-4o-mini";
        String GPT_4_TURBO = "gpt-4-turbo";
        String GPT_4_TURBO_PREVIEW = "gpt-4-turbo-preview";
        String TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002";
        String TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large";
        String TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small";
        String WHISPER_1 = "whisper-1";
        String TTS_1 = "tts-1";
        String DALLE_2 = "dall-e-2";
        String DALLE_3 = "dall-e-3";

        String QWEN_3_7_PLUS = "qwen3.7-plus";
        String QWEN_3_6_PLUS = "qwen3.6-plus";
        String QWEN_3_5_PLUS = "qwen3.5-plus";
        String TEXT_EMBEDDING_V3 = "text-embedding-v3";
        String TEXT_EMBEDDING_V4 = "text-embedding-v4";
        String PARAFORMER_REALTIME_V2 = "paraformer-realtime-v2";
        String FUN_ASR_REALTIME = "fun-asr-realtime";
        String GUMMY_REALTIME_V1 = "gummy-realtime-v1";
        String COSYVOICE_V1 = "cosyvoice-v1";
        String COSYVOICE_V2 = "cosyvoice-v2";
        String SAMBERT_V1 = "sambert-v1";
        String QWEN3_TTS_FLASH = "qwen3-tts-flash";
        String QWEN_TTS = "qwen-tts";
        String QWEN_IMAGE_PLUS = "qwen-image-plus";
        String GTE_RERANK = "gte-rerank";
        String QWEN3_RERANK = "qwen3-rerank";
        String QWEN3_TTS_PREFIX = "qwen3-tts";
        String SAMBERT_PREFIX = "sambert";
        String SAMBERT_NAME_PREFIX = "sambert-";
        String GUMMY_PREFIX = "gummy-";
        String WANX_PREFIX = "wanx-";
        String WAN2_PREFIX = "wan2.";

        String CLAUDE_3_OPUS = "claude-3-opus-20240229";
        String CLAUDE_3_SONNET = "claude-3-sonnet-20240229";
        String CLAUDE_3_HAIKU = "claude-3-haiku-20240307";
        String CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022";
        String CLAUDE_3_5_HAIKU = "claude-3-5-haiku-20241022";

        String DEEPSEEK_V4_FLASH = "deepseek-v4-flash";
        String DEEPSEEK_V4_PRO = "deepseek-v4-pro";
        String DEEPSEEK_CHAT = "deepseek-chat";
        String DEEPSEEK_REASONER = "deepseek-reasoner";

        String GEMINI_1_0_PRO = "gemini-1.0-pro";
        String GEMINI_1_0_PRO_VISIO = "gemini-1.0-pro-visio";
        String GEMINI_EMBEDDING_001 = "gemini-embedding-001";
        String GEMINI_1_5_FLASH = "gemini-1.5-flash";
        String GEMINI_1_5_PRO = "gemini-1.5-pro";

        String KIMI_K2_6 = "kimi-k2.6";
        String KIMI_K2_5 = "kimi-k2.5";
        String KIMI_K2_THINKING = "kimi-k2-thinking";
        String KIMI_K2_THINKING_TURBO = "kimi-k2-thinking-turbo";
        String MOONSHOT_V1_8K = "moonshot-v1-8k";
        String MOONSHOT_V1_32K = "moonshot-v1-32k";
        String MOONSHOT_V1_128K = "moonshot-v1-128k";

        String MINI_MAX_M2_7 = "MiniMax-M2.7";
        String MINI_MAX_M2_7_HIGHSPEED = "MiniMax-M2.7-highspeed";
        String MINI_MAX_M2_5 = "MiniMax-M2.5";
        String MINI_MAX_M2_5_HIGHSPEED = "MiniMax-M2.5-highspeed";

        String QWEN_7B = "qwen:7b";
        String LLAMA3_8B = "llama3:8b";
        String DEEPSEEK_R1_8B = "deepseek-r1:8b";
        String NOMIC_EMBED_TEXT = "nomic-embed-text";
        String LLAVA_7B = "llava:7b";
        String LLAVA_13B = "llava:13b";
        String X_Z_IMAGE_TURBO = "x/z-image-turbo";

        String DEEPSEEK_AI_V3_2 = "deepseek-ai/DeepSeek-V3.2";
        String PRO_KIMI_K2_5 = "Pro/moonshotai/Kimi-K2.5";
        String QWEN3_VL_32B_THINKING = "Qwen/Qwen3-VL-32B-Thinking";
        String PRO_GLM_4_7 = "Pro/zai-org/GLM-4.7";
        String PRO_MINI_MAX_M2_1 = "Pro/MiniMaxAI/MiniMax-M2.1";
        String HUNYUAN_MT_7B = "tencent/Hunyuan-MT-7B";
        String QWEN3_EMBEDDING_8B = "Qwen/Qwen3-Embedding-8B";
        String BAAI_BGE_M3 = "BAAI/bge-m3";
        String BCE_EMBEDDING_BASE_V1 = "netease-youdao/bce-embedding-base_v1";
        String QWEN3_RERANKER_8B = "Qwen/Qwen3-Reranker-8B";
        String BAAI_BGE_RERANKER_V2_M3 = "BAAI/bge-reranker-v2-m3";
        String BCE_RERANKER_BASE_V1 = "netease-youdao/bce-reranker-base_v1";
        String QWEN3_IMAGE = "Qwen/Qwen3-Image";
        String KOLORS = "Kwai-Kolors/Kolors";
        String TELE_SPEECH_ASR = "TeleAI/TeleSpeechASR";
        String SENSE_VOICE_SMALL = "FunAudioLLM/SenseVoiceSmall";
        String COSY_VOICE_2_0_5B = "FunAudioLLM/CosyVoice2-0.5B";

        String HY3 = "hy3";
        String HUNYUAN_PRO = "hunyuan-pro";
        String HUNYUAN_STANDARD = "hunyuan-standard";
        String HUNYUAN_LITE = "hunyuan-lite";
        String HUNYUAN_ROLE = "hunyuan-role";
        String HUNYUAN_FUNCTIONCALL = "hunyuan-functioncall";
        String HUNYUAN_CODE = "hunyuan-code";
        String HUNYUAN_EMBEDDING = "hunyuan-embedding";
        String HUNYUAN_VISION = "hunyuan-vision";
        String HUNYUAN_DIT = "hunyuan-dit";

        String DOUBAO_1_5_PRO_32K = "doubao-1-5-pro-32k-250115";
        String DOUBAO_SEED_1_6 = "doubao-seed-1-6-251015";
        String DOUBAO_SEED_1_6_FLASH = "doubao-seed-1-6-flash-250828";
        String DOUBAO_SEED_1_6_THINKING = "doubao-seed-1-6-thinking-250715";
        String DOUBAO_SEED_1_6_VISION = "doubao-seed-1-6-vision-250815";
        String DOUBAO_SEEDREAM_4_0 = "doubao-seedream-4-0-250828";
        String DOUBAO_SEEDREAM_4_5 = "doubao-seedream-4-5-251128";
        String DOUBAO_EMBEDDING_TEXT = "doubao-embedding-text-240715";

        String ERNIE_BOT_4 = "ERNIE-Bot-4";
        String ERNIE_BOT = "ERNIE-Bot";
        String ERNIE_BOT_TURBO = "ERNIE-Bot-turbo";
        String EMBEDDING_V1 = "Embedding-V1";

        String QWEN3_8B = "qwen3:8b";
        String BGE_M3 = "bge-m3";
        String SDXL_TURBO = "sdxl-turbo";
        String BGE_RERANKER_BASE = "bge-reranker-base";
        String CHAT_TTS = "ChatTTS";
        String WHISPER_LARGE_V3 = "whisper-large-v3";

        String XUNFEI_ULTRA_4_0 = "4.0Ultra";
        String XUNFEI_MAX_32K = "max-32k";
        String XUNFEI_GENERAL_V3_5 = "generalv3.5";
        String XUNFEI_GENERAL_V3 = "generalv3";
        String XUNFEI_LITE = "lite";

        String GLM_5_1 = "glm-5.1";
        String GLM_5 = "glm-5";
        String GLM_4 = "glm-4";
        String GLM_4V = "glm-4v";
        String GLM_3_TURBO = "glm-3-turbo";
        String EMBEDDING_3 = "embedding-3";
        String GLM_4V_PLUS = "glm-4v-plus";
        String GLM_4V_FLASH = "glm-4v-flash";
        String GLM_IMAGE = "glm-image";
        String COGVIEW_4 = "cogview-4";
        String COGVIEW_3_FLASH = "cogview-3-flash";
    }

    /**
     * 凭据表单字段键名
     */
    interface CredentialField {
        String BASE_URL = "baseUrl";
        String API_KEY = "apiKey";
    }

    /**
     * 模型参数键名
     */
    interface ParamKey {
        String TEMPERATURE = "temperature";
        String MAX_TOKENS = "max_tokens";
        String ENABLE_THINKING = "enable_thinking";
        String THINKING = "thinking";
        String TYPE = "type";
        String DIMENSIONS = "dimensions";
        String SIZE = "size";
        String QUALITY = "quality";
        String N = "n";
        String STEPS = "steps";
        String SEED = "seed";
        String STYLE = "style";
        String NEGATIVE_PROMPT = "negative_prompt";
        String PROMPT_EXTEND = "prompt_extend";
        String WATERMARK = "watermark";
        String VOICE = "voice";
        String VOLUME = "volume";
        String SPEECH_RATE = "speechRate";
        String TARGET_LANGUAGE = "targetLanguage";
        String LANGUAGE_HINTS = "language_hints";
        String DISFLUENCY_REMOVAL_ENABLED = "disfluency_removal_enabled";
        String SHOW_PUNCTUATION = "show_punctuation";
        String INVERSE_TEXT_NORMALIZATION = "inverse_text_normalization";
        String FIELD = "field";
        String DEFAULT_VALUE = "default_value";
        String MAX_RETRIES = "maxRetries";
    }

    /**
     * 常用取值 / 默认值
     */
    interface Value {
        String ENABLED = "enabled";
        String DISABLED = "disabled";
        String MP3 = "mp3";
        String AUTO = "auto";
        String ZH = "zh";
        String EN = "en";
        String CHERRY = "CHERRY";
    }

    /**
     * 请求与响应 JSON 字段名
     */
    interface RequestField {
        String MODEL = "model";
        String PROMPT = "prompt";
        String BATCH_SIZE = "batch_size";
        String IMAGE_SIZE = "image_size";
        String IMAGE = "image";
        String IMAGES = "images";
        String URL = "url";
        String QUERY = "query";
        String DOCUMENTS = "documents";
        String TOP_N = "top_n";
        String RESULTS = "results";
        String INDEX = "index";
        String RELEVANCE_SCORE = "relevance_score";
        String TEXT = "text";
    }

    /**
     * HTTP 头、媒体类型与端点
     */
    interface Http {
        String CONTENT_TYPE = "Content-Type";
        String APPLICATION_JSON = "application/json";
        String CHARSET_UTF_8 = "; charset=UTF-8";
        String ENDPOINT_RERANK = "/rerank";
        String ENDPOINT_IMAGES_GENERATIONS = "/images/generations";
    }

    /**
     * 控制器路由
     */
    interface ApiPath {
        String MODEL = "/model";
        String MODEL_LIST = "/model_list";
        String MODEL_ID = "/model/{id}";
        String MODEL_PARAMS_FORM = "/model/{id}/model_params_form";
        String PROVIDER = "/provider";
        String PROVIDER_MODEL_TYPE_LIST = "/provider/model_type_list";
        String PROVIDER_MODEL_FORM = "/provider/model_form";
        String PROVIDER_MODEL_PARAMS_FORM = "/provider/model_params_form";
        String PROVIDER_MODEL_LIST = "/provider/model_list";
    }

    /**
     * 资源标识
     */
    interface Resource {
        String MODEL = "model";
        String SHARED_MODEL = "shared_model";
    }

    /**
     * 业务消息码
     */
    interface MessageCode {
        String MODEL_NAME_EXISTS = "model.name.exists";
        String MODEL_NAME_NOT_FOUND = "model.name.not.found";
    }

    /**
     * 静态资源路径
     */
    interface IconPath {
        String MODEL_ICONS = "model-icons/";
    }

    /**
     * 文件相关标记
     */
    interface FileToken {
        String AUDIO_TEMP_PREFIX = "audio_temp_";
        String DOT = ".";
        String DATA_URI_PREFIX = "data:";
        String DATA_URI_BASE64_SEPARATOR = ";base64,";
        String CACHE_KEY_SEPARATOR = "|";
    }
}
