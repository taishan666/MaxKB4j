package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.custom.model.BaiLianImageModel;
import com.maxkb4j.model.custom.model.BaiLianReranker;
import com.maxkb4j.model.custom.model.BaiLianSTTModel;
import com.maxkb4j.model.custom.model.BaiLianTTSModel;
import com.maxkb4j.model.custom.params.*;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.ISTTModel;
import com.maxkb4j.model.service.ITTSModel;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.community.model.dashscope.QwenModelName;
import dev.langchain4j.community.model.dashscope.WanxModelName;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * AliYun BaiLian (DashScope) Model Provider
 */
@Component
@ModelProviderType(provider = Provider.ALI_YUN_BAI_LIAN, name = "阿里百练", icon = Provider.ICON_ALI_YUN_BAI_LIAN)
public class AliYunBaiLianModelProvider extends OpenAiModelProvider {

    private static final String BASE_URL = BaseUrl.ALI_YUN_BAI_LIAN;

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
    public String getDefaultBaseUrl(){
        return BASE_URL;
    }


    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
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
    public ISTTModel buildSTTModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianSTTModel(modelName, credential, params);
    }

    @Override
    public ITTSModel buildTTSModel(String modelName, ModelCredential credential, JSONObject params) {
        return new BaiLianTTSModel(modelName, credential, params);
    }

    @Override
    protected List<BaseField> getEmbeddingModelParamsForm() {
        return new EmbeddingModelParams().toForm();
    }
}
