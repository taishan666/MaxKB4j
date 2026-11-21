package com.maxkb4j.model.provider;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.form.SliderField;
import com.maxkb4j.model.vo.ModelInfo;
import dev.langchain4j.model.ModelDisabledException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.DisabledStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.DisabledEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.DisabledImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.CredentialField;
import static com.maxkb4j.model.consts.ModelConstants.ParamKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AbsModelProvider} 纯逻辑测试：参数提取、能力/模型信息查询、参数表单分派与默认实现。
 */
class AbsModelProviderTest {

    private final StubProvider provider = new StubProvider();

    @Test
    void getDoubleParam_nullParamsOrMissingKey_returnsNull() {
        assertNull(provider.getDoubleParam(null, ParamKey.TEMPERATURE));
        assertNull(provider.getDoubleParam(new JSONObject(), ParamKey.TEMPERATURE));
        JSONObject params = new JSONObject();
        params.put(ParamKey.TEMPERATURE, 0.7);
        assertEquals(0.7, provider.getDoubleParam(params, ParamKey.TEMPERATURE));
    }

    @Test
    void getIntParam_nullParamsOrMissingKey_returnsNull() {
        assertNull(provider.getIntParam(null, ParamKey.MAX_TOKENS));
        assertNull(provider.getIntParam(new JSONObject(), ParamKey.MAX_TOKENS));
        JSONObject params = new JSONObject();
        params.put(ParamKey.MAX_TOKENS, 1024);
        assertEquals(1024, provider.getIntParam(params, ParamKey.MAX_TOKENS));
    }

    @Test
    void getStringParam_nullParamsOrMissingKey_returnsNull() {
        assertNull(provider.getStringParam(null, "key"));
        assertNull(provider.getStringParam(new JSONObject(), "key"));
        JSONObject params = new JSONObject();
        params.put("key", "value");
        assertEquals("value", provider.getStringParam(params, "key"));
    }

    @Test
    void getBooleanParam_nullParamsOrMissingKey_defaultsToFalse() {
        assertFalse(provider.getBooleanParam(null, "key"));
        assertFalse(provider.getBooleanParam(new JSONObject(), "key"));
        JSONObject params = new JSONObject();
        params.put("key", true);
        assertTrue(provider.getBooleanParam(params, "key"));
    }

    @Test
    void isSupport_matchesByModelType() {
        assertTrue(provider.isSupport(ModelType.LLM));
        assertFalse(provider.isSupport(ModelType.EMBEDDING));
        assertFalse(provider.isSupport(ModelType.TTS));
    }

    @Test
    void getModelInfo_matchesTypeAndName() {
        ModelInfo info = provider.getModelInfo(ModelType.LLM, "stub-model");
        assertEquals("stub-model", info.getName());
        assertEquals(ModelType.LLM, info.getModelType());
        assertNull(provider.getModelInfo(ModelType.LLM, "other"));
        assertNull(provider.getModelInfo(ModelType.EMBEDDING, "stub-model"));
    }

    @Test
    void getModelParamsForm_dispatchesByModelType() {
        List<BaseField> chatForm = provider.getModelParamsForm(ModelType.LLM.getKey());
        assertEquals(List.of(ParamKey.TEMPERATURE, ParamKey.MAX_TOKENS, ParamKey.ENABLE_THINKING),
                chatForm.stream().map(BaseField::getField).toList());

        assertEquals(chatForm.stream().map(BaseField::getField).toList(),
                provider.getModelParamsForm(ModelType.VISION.getKey())
                        .stream().map(BaseField::getField).toList());

        // 基类默认没有向量参数表单
        assertTrue(provider.getModelParamsForm(ModelType.EMBEDDING.getKey()).isEmpty());

        List<BaseField> imageForm = provider.getModelParamsForm(ModelType.TTI.getKey());
        assertEquals(1, imageForm.size());
        assertEquals(ParamKey.SIZE, imageForm.get(0).getField());

        assertTrue(provider.getModelParamsForm(ModelType.TTS.getKey()).isEmpty());
        assertTrue(provider.getModelParamsForm(null).isEmpty());
    }

    @Test
    void defaultBuildMethods_returnDisabledModelsOrThrow() {
        assertInstanceOf(DisabledChatModel.class, provider.buildChatModel("m", credential(), new JSONObject()));
        assertInstanceOf(DisabledStreamingChatModel.class,
                provider.buildStreamingChatModel("m", credential(), new JSONObject()));
        assertInstanceOf(DisabledEmbeddingModel.class,
                provider.buildEmbeddingModel("m", credential(), new JSONObject()));
        assertInstanceOf(DisabledImageModel.class,
                provider.buildImageModel("m", credential(), new JSONObject()));
        assertThrows(ModelDisabledException.class, () -> provider.buildScoringModel("m", credential(), new JSONObject()));
        assertThrows(ModelDisabledException.class, () -> provider.buildSTTModel("m", credential(), new JSONObject()));
        assertThrows(ModelDisabledException.class, () -> provider.buildTTSModel("m", credential(), new JSONObject()));
    }

    @Test
    void modelIsValid_unsupportedOrNullType_isNoOp() {
        // STT/空类型不在校验范围内，不应触发任何构建与网络调用
        provider.modelIsValid(ModelType.STT.getKey(), "m", credential(), new JSONObject());
        provider.modelIsValid(null, "m", credential(), new JSONObject());
    }

    @Test
    void defaultCredentialForm_hidesBaseUrlAndShowsApiKey() {
        List<BaseField> form = provider.getModelCredential().toForm();
        assertEquals(1, form.size());
        assertEquals(CredentialField.API_KEY, form.get(0).getField());
    }

    private static ModelCredential credential() {
        ModelCredential credential = new ModelCredential();
        credential.setBaseUrl("https://example.com/v1");
        credential.setApiKey("sk-test");
        return credential;
    }

    /**
     * 仅提供一个 LLM 模型条目的最小实现，暴露受保护的参数提取方法供同包测试调用。
     */
    static final class StubProvider extends AbsModelProvider {

        Double doubleParam(JSONObject params, String key) {
            return getDoubleParam(params, key);
        }

        Integer intParam(JSONObject params, String key) {
            return getIntParam(params, key);
        }

        String stringParam(JSONObject params, String key) {
            return getStringParam(params, key);
        }

        Boolean booleanParam(JSONObject params, String key) {
            return getBooleanParam(params, key);
        }

        @Override
        public List<ModelInfo> getModelList() {
            return List.of(new ModelInfo("stub-model", "测试模型", ModelType.LLM));
        }
    }
}
