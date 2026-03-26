package com.maxkb4j.model.enums;

import com.maxkb4j.model.provider.*;
import com.maxkb4j.model.vo.ModelProviderInfo;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ModelProvider {
    AliYunBaiLian("阿里百练", "AliYunBaiLian", "qwen_icon.svg"),
    Anthropic("Anthropic", "Anthropic", "anthropic_icon.svg"),
    Azure("Azure OpenAI", "Azure","azure_icon.svg"),
    DeepSeek("DeepSeek", "DeepSeek", "deepseek_icon.svg"),
    Gemini("Google Gemini", "Gemini","gemini_icon.svg"),
    Kimi("Kimi", "Kimi","kimi_icon.svg"),
    OpenAI("OpenAI", "OpenAI","openai_icon.svg"),
    SiliconFlow("Silicon Flow", "SiliconFlow","silicon_flow_icon.svg"),
    Tencent("腾讯混元", "Tencent","tencent_icon.svg"),
    VolcanicEngine("火山引擎", "VolcanicEngine","volcanic_engine_icon.svg"),
    WenXin("文心一言", "WenXin","wenxin_icon.svg"),
    XunFei("讯飞星火", "XunFei", "xf_icon.svg"),
    ZhiPu("智谱清言", "ZhiPu", "zhipu_ai_icon.svg"),
    Local("本地模型", "LocalModel", "local_icon.svg"),
   // LocalAI("LocalAI", "LocalAI", "local_ai_icon.svg"),
    OLlama("OLlama", "OLlama","ollama_icon.svg"),
    XInference("Xorbits Inference", "XInference","xinference_icon.svg");

    private final String name;
    private final String provider;
    private final String icon;
    private final AbsModelProvider modelProvider;
    // 缓存 ModelProviderInfo 对象，避免重复创建
    private volatile ModelProviderInfo cachedInfo;

    ModelProvider(String name, String provider, String icon) {
        this.name = name;
        this.provider = provider;
        this.icon = icon;
        this.modelProvider = createModelProvider();
    }

    /**
     * 创建对应的 ModelProvider 实例
     * 使用延迟创建避免构造函数中的耗时操作
     */
    private AbsModelProvider createModelProvider() {
        return switch (this) {
            case AliYunBaiLian -> new AliYunBaiLianModelProvider();
            case Anthropic -> new AnthropicProvider();
            case Azure -> new AzureModelProvider();
            case DeepSeek -> new DeepSeekModelProvider();
            case Gemini -> new GeminiModelProvider();
            case Kimi -> new KimiModelProvider();
            case OpenAI -> new OpenAiModelProvider();
            case SiliconFlow -> new SiliconFlowModelProvider();
            case Tencent -> new TencentModelProvider();
            case VolcanicEngine -> new VolcanicEngineModelProvider();
            case WenXin -> new WenXinModelProvider();
            case XunFei -> new XunFeiModelProvider();
            case ZhiPu -> new ZhiPuModelProvider();
            case Local -> new LocalModelProvider();
            case OLlama -> new OLlamaModelProvider();
            case XInference -> new XInferenceModelProvider();
        };
    }

    private static final Map<String, AbsModelProvider> PROVIDER_MAP = new HashMap<>();

    static {
        for (ModelProvider value : ModelProvider.values()) {
            PROVIDER_MAP.put(value.provider, value.modelProvider);
        }
    }

    public static AbsModelProvider get(String provider) {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }
        return PROVIDER_MAP.getOrDefault(provider, null);
    }


    public ModelProviderInfo getInfo() {
        if (cachedInfo == null) {
            synchronized (this) {
                if (cachedInfo == null) {
                    cachedInfo = new ModelProviderInfo(this.provider, this.name, this.icon);
                }
            }
        }
        return cachedInfo;
    }
}
