package com.tarzan.maxkb4j.module.model.provider.enums;

import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.*;
import lombok.Getter;

import java.util.*;

@Getter
public enum ModelProviderEnum {
    AliYunBaiLian("阿里百练","AliYunBaiLian", new AliYunBaiLianModelProvider()),
    Anthropic("Anthropic","Anthropic", new AnthropicProvider()),
    Azure("Azure OpenAI","Azure", new AzureModelProvider()),
    DeepSeek("DeepSeek","DeepSeek", new DeepSeekModelProvider()),
    Gemini("Google Gemini","Gemini", new GeminiModelProvider()),
    Kimi("Kimi","Kimi", new KimiModelProvider()),
    OpenAI("OpenAI","OpenAI", new OpenAiModelProvider()),
    Tencent("腾讯混元","Tencent", new TencentModelProvider()),
    VolcanicEngine("火山引擎","VolcanicEngine", new VolcanicEngineModelProvider()),
    WenXin("文心一言","WenXin", new WenXinModelProvider()),
    XunFei("讯飞星火","XunFei", new XunFeiModelProvider()),
    ZhiPu("智谱清言","ZhiPu", new ZhiPuModelProvider()),
    Local("本地模型","LocalModel", new LocalModelProvider()),
    LocalAI("LocalAI","LocalAI", new LocalAIModelProvider()),
    OLlama("OLlama","OLlama", new OLlamaModelProvider()),
    XInference("Xorbits Inference","XInference", new XInferenceModelProvider());

    private final String name;
    private final String provider;
    private final IModelProvider modelProvider;

    ModelProviderEnum(String name, String provider, IModelProvider modelProvider) {
        this.name = name;
        this.provider = provider;
        this.modelProvider = modelProvider;
    }

    private static final Map<String, IModelProvider> PROVIDER_MAP = new HashMap<>();

    static {
        for (ModelProviderEnum value : ModelProviderEnum.values()) {
            PROVIDER_MAP.put(value.provider, value.modelProvider);
        }
    }

    public static IModelProvider get(String provider) {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }
        return PROVIDER_MAP.getOrDefault(provider, null);
    }

    public static List<IModelProvider> getAllProvider() {
        return Arrays.stream(values())
                .filter(e->!e.equals(ModelProviderEnum.LocalAI)&&!e.equals(ModelProviderEnum.XunFei))
                .map(e -> e.modelProvider)
                .toList();
    }
}
