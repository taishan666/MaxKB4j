package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.base.STTModel;
import com.maxkb4j.model.base.TTSModel;
import com.maxkb4j.model.custom.model.*;
import com.maxkb4j.model.custom.params.*;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.WanxModelName;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * AliYun BaiLian (DashScope) Model Provider
 */
@Component
@ModelProviderType(provider = Provider.ALI_YUN_BAI_LIAN, name = "阿里百练", icon = Provider.ICON_ALI_YUN_BAI_LIAN)
public class AliYunBaiLianModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.ALI_YUN_BAI_LIAN;

    /**
     * OpenAI 兼容模式地址后缀，需要转换为 DashScope 原生地址后缀
     */
    private static final String COMPATIBLE_MODE_SUFFIX = "/compatible-mode/v1";
    private static final String NATIVE_SUFFIX = "/api/v1";

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.QWEN_3_7_PLUS, "", ModelType.LLM),
            new ModelInfo(ModelName.QWEN_3_6_PLUS, "", ModelType.LLM),
            new ModelInfo(ModelName.QWEN_3_5_PLUS, "", ModelType.LLM),
            new ModelInfo(QwenModelName.QWEN_PLUS, "", ModelType.LLM),
            new ModelInfo(QwenModelName.QWEN_MAX, "", ModelType.LLM),
            new ModelInfo(ModelName.TEXT_EMBEDDING_V3, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.TEXT_EMBEDDING_V4, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.PARAFORMER_REALTIME_V2, "", ModelType.STT),
            new ModelInfo(ModelName.FUN_ASR_REALTIME, "", ModelType.STT),
            new ModelInfo(ModelName.GUMMY_REALTIME_V1, "", ModelType.STT, new GummySTTParams()),
            new ModelInfo(ModelName.COSYVOICE_V1, "", ModelType.TTS, new CosyVoiceV1TTSParams()),
            new ModelInfo(ModelName.COSYVOICE_V2, "", ModelType.TTS, new CosyVoiceV2TTSParams()),
            new ModelInfo(ModelName.SAMBERT_V1, "", ModelType.TTS, new SamBertTTSParams()),
            new ModelInfo(ModelName.QWEN3_TTS_FLASH, "", ModelType.TTS, new QWenTTSParams()),
            new ModelInfo(ModelName.QWEN_TTS, "", ModelType.TTS, new QWenTTSParams()),
            new ModelInfo(ModelName.QWEN_3_6_PLUS, "", ModelType.VISION),
            new ModelInfo(ModelName.QWEN_3_5_PLUS, "", ModelType.VISION),
            new ModelInfo(QwenModelName.QWEN_VL_PLUS, "", ModelType.VISION),
            new ModelInfo(QwenModelName.QWEN_VL_MAX, "", ModelType.VISION),
            new ModelInfo(WanxModelName.WANX2_1_T2I_TURBO, "", ModelType.TTI, new WanXImageModelParams()),
            new ModelInfo(WanxModelName.WANX2_1_T2I_PLUS, "", ModelType.TTI, new WanXImageModelParams()),
            new ModelInfo(ModelName.QWEN_IMAGE_PLUS, "", ModelType.TTI, new QwenImageModelParams()),
            new ModelInfo(ModelName.GTE_RERANK, "", ModelType.RERANKER),
            new ModelInfo(ModelName.QWEN3_RERANK, "", ModelType.RERANKER)
    );

    @Override
    public String getDefaultBaseUrl() {
        return BASE_URL;
    }

    /**
     * 获取 DashScope 原生 SDK 使用的 baseUrl
     * <p>
     * DashScope 原生 SDK（TextEmbedding / MultiModalEmbedding 等）会把 baseUrl 作为前缀，
     * 拼接 {@code /services/{taskGroup}/{task}/{function}} 得到最终请求地址，因此必须使用
     * {@code .../api/v1} 形式。
     * <p>
     * 而本 Provider 默认保存/展示的是 OpenAI 兼容模式地址 {@code .../compatible-mode/v1}，
     * 若直接传给原生 SDK，实际请求会落到
     * {@code .../compatible-mode/v1/services/embeddings/text-embedding/text-embedding}，
     * 网关返回 {@code 404 Not Found}。此方法负责把兼容模式地址归一化为原生地址。
     *
     * @param baseUrl 用户配置或默认的 baseUrl（可能是 OpenAI 兼容模式地址）
     * @return DashScope 原生 SDK 可直接使用的 baseUrl
     */
    public String getDashScopeBaseUrl(String baseUrl) {
        String url = getBaseUrl(baseUrl);
        // 去掉末尾斜杠，避免拼接出 //services
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith(COMPATIBLE_MODE_SUFFIX)) {
            url = url.substring(0, url.length() - COMPATIBLE_MODE_SUFFIX.length()) + NATIVE_SUFFIX;
        }
        return url;
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return QwenMultiModalEmbeddingModel.builder()
                .baseUrl(getDashScopeBaseUrl(credential.getBaseUrl()))
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .dimension(getIntParam(params, ParamKey.DIMENSIONS))
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianImageModel(modelName, credential, params);
    }

    @Override
    public ScoringModel buildScoringModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianReranker(modelName, credential, params);
    }

    @Override
    public STTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianSTTModel(modelName, credential, params);
    }

    @Override
    public TTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianTTSModel(modelName, credential, params);
    }

    @Override
    protected List<BaseField> getEmbeddingModelParamsForm() {
        return new EmbeddingModelParams().toForm();
    }
}
