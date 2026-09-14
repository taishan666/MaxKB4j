package com.maxkb4j.model.custom.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.embedding.request.EmbeddingParameter;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.request.EmbeddingRequestParameters;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 多模态向量模型的公共抽象基类。
 *
 * <p>采用模板方法模式，将各 provider 共享的逻辑（多模态判定、支持的参数/内容类型、
 * {@link #doEmbed} 主流程）统一上移到本类；子类只需实现 provider 特有的
 * 单条输入向量化（{@link #embedMultimodal}）与纯文本委托模型（{@link #textEmbeddingModel}）。</p>
 */
public abstract class AbstractMultiModalEmbeddingModel extends DimensionAwareEmbeddingModel {

    protected final String modelName;

    protected AbstractMultiModalEmbeddingModel(String modelName) {
        this.modelName = modelName;
    }

    /**
     * 判断给定模型名是否为多模态模型。
     */
    protected static boolean isMultimodal(String modelName) {
        return modelName != null
                && (modelName.contains("-vl-") || modelName.contains("-vision-") || modelName.endsWith("-vision"));
    }

    @Override
    public Set<EmbeddingParameter<?>> supportedParameters() {
        return Set.of(EmbeddingRequestParameters.INPUT_TYPE, EmbeddingRequestParameters.DIMENSIONS);
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return isMultimodal(modelName) ? Set.of(ContentType.TEXT, ContentType.IMAGE) : Set.of(ContentType.TEXT);
    }

    @Override
    public EmbeddingResponse doEmbed(EmbeddingRequest request) {
        if (!isMultimodal(modelName)) {
            return textEmbeddingModel().doEmbed(request);
        }
        List<Embedding> embeddings = new ArrayList<>();
        int tokenCount = 0;
        for (EmbeddingInput input : request.inputs()) {
            EmbeddingResult result = embedMultimodal(input);
            embeddings.add(result.embedding());
            tokenCount += result.tokenCount();
        }
        return EmbeddingResponse.builder()
                .modelName(modelName)
                .embeddings(embeddings)
                .tokenUsage(new TokenUsage(tokenCount))
                .build();
    }

    /**
     * Ark 多模态 embedding 的 image_url.url 仅支持 http/https 链接，
     * 或 data:<mimeType>;base64,<data> 形式的 Data URI（裸 base64 会被服务端以
     * InvalidParameter.UnsupportedInput 拒绝）
     */
    protected static String toBase64ImageUrl(Image image) {
        String base64Data = image.base64Data();
        if (!Utils.isNullOrBlank(base64Data)) {
            if (base64Data.startsWith("data:")) {
                return base64Data;
            }
            String mimeType = Utils.isNullOrBlank(image.mimeType()) ? "image/png" : image.mimeType();
            return "data:" + mimeType + ";base64," + base64Data;
        }
        String url = image.url() == null ? null : image.url().toString();
        if (!Utils.isNullOrBlank(url)) {
            return url;
        }
        throw new IllegalArgumentException("Image content contains neither base64 data nor URL");
    }

    /**
     * 纯文本场景下委托的向量模型。
     */
    protected abstract DimensionAwareEmbeddingModel textEmbeddingModel();

    /**
     * 对单条多模态输入执行向量化，由子类基于各自 SDK 实现。
     */
    protected abstract EmbeddingResult embedMultimodal(EmbeddingInput input);

    /**
     * 单条输入的向量化结果，包含向量与消耗的 token 数。
     */
    protected record EmbeddingResult(Embedding embedding, int tokenCount) {
    }
}
