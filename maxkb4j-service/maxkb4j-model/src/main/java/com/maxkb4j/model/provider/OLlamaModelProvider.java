package com.maxkb4j.model.provider;
import com.maxkb4j.model.annotation.ModelProviderType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.custom.credential.ModelCredentialForm;
import com.maxkb4j.model.custom.params.OLlamaChatModelParams;
import com.maxkb4j.model.custom.params.OllamaImageModelParams;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaImageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Ollama Model Provider - Local deployment
 */
@Component
@ModelProviderType(provider = Provider.OLLAMA, name = "OLlama", icon = Provider.ICON_OLLAMA)
public class OLlamaModelProvider extends AbsModelProvider {

    private static final String BASE_URL = BaseUrl.OLLAMA;
    private static final List<ModelInfo> MODEL_INFOS = List.of(
            new ModelInfo(ModelName.QWEN_7B, "", ModelType.LLM),
            new ModelInfo(ModelName.LLAMA3_8B, "", ModelType.LLM),
            new ModelInfo(ModelName.DEEPSEEK_R1_8B, "", ModelType.LLM),
            new ModelInfo(ModelName.NOMIC_EMBED_TEXT, "", ModelType.EMBEDDING),
            new ModelInfo(ModelName.LLAVA_7B, "", ModelType.VISION),
            new ModelInfo(ModelName.LLAVA_13B, "", ModelType.VISION),
            new ModelInfo(ModelName.X_Z_IMAGE_TURBO, "", ModelType.TTI)
    );

    /**
     * 重写父类方法，为 Ollama 提供 UTF-8 编码支持的 HTTP 客户端
     */
    @Override
    protected HttpClientBuilder getHttpClientBuilder() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE + Http.CHARSET_UTF_8)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return SpringRestClient.builder()
                .restClientBuilder(restClientBuilder);
    }

    @Override
    public List<BaseField> getChatModelParamsForm() {
        return new OLlamaChatModelParams().toForm();
    }

    @Override
    protected List<BaseField> getImageModelParamsForm() {
        return new OllamaImageModelParams().toForm();
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(false, BASE_URL);
    }

    @Override
    public List<ModelInfo> getModelList() {
        return MODEL_INFOS;
    }

    @Override
    public ChatModel buildChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .think(getBooleanParam(params,ParamKey.ENABLE_THINKING))
                .returnThinking(true)
                .build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaStreamingChatModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .temperature(getDoubleParam(params, ParamKey.TEMPERATURE))
                .think(getBooleanParam(params,ParamKey.ENABLE_THINKING))
                .returnThinking(true)
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(String modelName, ModelCredential credential, JSONObject params) {
        return OllamaEmbeddingModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .build();
    }

    @Override
    public ImageModel buildImageModel(String modelName, ModelCredential credential, JSONObject params) {
        return  OllamaImageModel.builder()
                .httpClientBuilder(getHttpClientBuilder())
                .baseUrl(credential.getBaseUrl())
                .modelName(modelName)
                .width(1024)
                .height(768)
                .steps(params.getInteger(ParamKey.STEPS))
                .seed(params.getInteger(ParamKey.SEED))
                .build();
    }
}
