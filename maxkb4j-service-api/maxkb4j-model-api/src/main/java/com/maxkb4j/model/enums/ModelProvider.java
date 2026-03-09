package com.maxkb4j.model.enums;

import com.maxkb4j.model.service.AbsModelProvider;
import com.maxkb4j.model.service.impl.*;
import com.maxkb4j.model.vo.ModelProviderInfo;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ModelProvider {
    AliYunBaiLian("阿里百练", "AliYunBaiLian", "qwen_icon.svg",new AliYunBaiLianModelProvider()),
    Anthropic("Anthropic", "Anthropic", "anthropic_icon.svg",new AnthropicProvider()),
    Azure("Azure OpenAI", "Azure","azure_icon.svg", new AzureModelProvider()),
    DeepSeek("DeepSeek", "DeepSeek", "deepseek_icon.svg",new DeepSeekModelProvider()),
    Gemini("Google Gemini", "Gemini","gemini_icon.svg", new GeminiModelProvider()),
    Kimi("Kimi", "Kimi","kimi_icon.svg", new KimiModelProvider()),
    OpenAI("OpenAI", "OpenAI","openai_icon.svg", new OpenAiModelProvider()),
    SiliconFlow("Silicon Flow", "SiliconFlow","silicon_flow_icon.svg", new SiliconFlowModelProvider()),
    Tencent("腾讯混元", "Tencent","tencent_icon.svg", new TencentModelProvider()),
    VolcanicEngine("火山引擎", "VolcanicEngine","volcanic_engine_icon.svg", new VolcanicEngineModelProvider()),
    WenXin("文心一言", "WenXin","wenxin_icon.svg", new WenXinModelProvider()),
    XunFei("讯飞星火", "XunFei", "xf_icon.svg",new XunFeiModelProvider()),
    ZhiPu("智谱清言", "ZhiPu", "zhipu_ai_icon.svg",new ZhiPuModelProvider()),
    Local("本地模型", "LocalModel", "local_icon.svg",new LocalModelProvider()),
   // LocalAI("LocalAI", "LocalAI", "local_ai_icon.svg",new LocalAIModelProvider()),
    OLlama("OLlama", "OLlama","ollama_icon.svg", new OLlamaModelProvider()),
    XInference("Xorbits Inference", "XInference","xinference_icon.svg", new XInferenceModelProvider());

    private final String name;
    private final String provider;
    private final String icon;
    private final AbsModelProvider modelProvider;

    ModelProvider(String name, String provider, String icon, AbsModelProvider modelProvider) {
        this.name = name;
        this.provider = provider;
        this.icon = icon;
        this.modelProvider = modelProvider;
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
        return new ModelProviderInfo(this);
    }
}
