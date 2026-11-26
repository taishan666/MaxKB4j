package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.integration.chat.OllamaChatModelParams;
import com.maxkb4j.model.integration.credential.ModelCredentialForm;
import com.maxkb4j.model.integration.image.OllamaImageModelParams;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaImageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Ollama Model Provider - Local deployment
 */
@Component
@ModelProviderType(provider = Provider.OLLAMA, name = "OLlama", icon = Provider.ICON_OLLAMA)
public class OLlamaModelProvider extends AbsModelProvider {

    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.QWEN_7B, "", ModelType.LLM),
            new ModelInfo(ModelName.LLAMA3_8B, "", ModelType.LLM),
            new ModelInfo(ModelName.DEEPSEEK_R1_8B, "", ModelType.LLM),
            new ModelInfo(ModelName.NOMIC_EMBED_TEXT, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.LLAVA_7B, "", ModelType.VISION),
            new ModelInfo(ModelName.LLAVA_13B, "", ModelType.VISION),
            new ModelInfo(ModelName.X_Z_IMAGE_TURBO, "", ModelType.TTI)
    );


    @Override
    public List<BaseField> getChatModelParamsForm() {
        return new OllamaChatModelParams().toForm();
    }

    @Override
    protected List<BaseField> getImageModelParamsForm() {
        return new OllamaImageModelParams().toForm();
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(false, BaseUrl.OLLAMA);
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaChatModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .think(getBooleanParam(params, ParamKey.ENABLE_THINKING))
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaStreamingChatModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .think(getBooleanParam(params, ParamKey.ENABLE_THINKING))
                .returnThinking(true)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaEmbeddingModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaImageModel.builder()
                .httpClientBuilder(getSpringRestBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .width(1024)
                .height(768)
                .steps(getIntParam(params, ParamKey.STEPS))
                .seed(getIntParam(params, ParamKey.SEED))
                .build();
    }
}
