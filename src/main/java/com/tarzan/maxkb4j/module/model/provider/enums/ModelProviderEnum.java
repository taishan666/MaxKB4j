package com.tarzan.maxkb4j.module.model.provider.enums;

import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.awsbedrockmodelprovider.AwsBedrockModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.AzureModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider.DeepSeekModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.geminimodelprovider.GeminiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.kimimodelprovider.KimiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.localmodelprovider.LocalModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.ollamamodelprovider.OLlamaModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.OpenaiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.qwenmodelprovider.QwenModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.tencentmodelprovider.TencentModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.vllmmodelprovider.VLlmModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.volcanicenginemodelprovider.VolcanicEngineModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.wenxinmodelprovider.WenXinModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xfmodelprovider.XfModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.XInferenceModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.ZhiPuModelProvider;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ModelProviderEnum {
    AliYunBaiLian("阿里百练","AliYunBaiLian", new AliYunBaiLianModelProvider()),
    AwsBedrock("Amazon Bedrock","AwsBedrock", new AwsBedrockModelProvider()),
    Azure("Azure OpenAI","Azure", new AzureModelProvider()),
    DeepSeek("DeepSeek","DeepSeek", new DeepSeekModelProvider()),
    Gemini("Google DeepMind","Gemini", new GeminiModelProvider()),
    Kimi("月之暗面","Kimi", new KimiModelProvider()),
    Local("本地模型","Local", new LocalModelProvider()),
    OLlama("OLlama","OLlama", new OLlamaModelProvider()),
    Openai("OpenAI","Openai", new OpenaiModelProvider()),
    QWen("通义千问","QWen", new QwenModelProvider()),
    Tencent("腾讯混元","Tencent", new TencentModelProvider()),
    VLlm("","VLlm", new VLlmModelProvider()),
    VolcanicEngine("火山引擎","VolcanicEngine", new VolcanicEngineModelProvider()),
    WenXin("文心一言","WenXin", new WenXinModelProvider()),
    XunFei("讯飞星火","XunFei", new XfModelProvider()),
    XInference("Xorbits Inference","XInference", new XInferenceModelProvider()),
    ZhiPu("智谱清言","ZhiPu", new ZhiPuModelProvider());

    private final String name;
    private final String provider;
    private final IModelProvider modelProvider;

    ModelProviderEnum(String name, String provider, IModelProvider modelProvider) {
        this.name = name;
        this.provider = provider;
        this.modelProvider = modelProvider;
    }

    public static IModelProvider get(String provider) {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }
        return getMap().getOrDefault(provider, null);
    }
    /**
     * 获取模型提供者的映射。
     *
     * @return 包含所有模型提供者名称和其实例的映射
     */
    public static Map<String, IModelProvider> getMap() {
        Map<String, IModelProvider> map = new HashMap<>();
        for (ModelProviderEnum providerEnum : values()) {
            map.put(providerEnum.getProvider(), providerEnum.getModelProvider());
        }
        return map;
    }
}
