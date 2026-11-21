package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.exception.ModelNotFoundException;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import com.maxkb4j.model.service.IModelInternalService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ModelProviderServiceImpl} 工厂逻辑测试：模型查找/供应商解析的异常路径与委托行为。
 */
@ExtendWith(MockitoExtension.class)
class ModelProviderServiceImplTest {

    private static final String MODEL_ID = "model-1";

    @Mock
    private IModelInternalService modelService;
    @Mock
    private ModelProviderRegistry providerRegistry;
    @Mock
    private AbsModelProvider absProvider;
    @InjectMocks
    private ModelProviderServiceImpl service;

    @Test
    void buildChatModel_blankId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.buildChatModel(" ", new JSONObject()));
        assertThrows(IllegalArgumentException.class, () -> service.buildChatModel(null, new JSONObject()));
    }

    @Test
    void buildChatModel_modelNotFound_throwsModelNotFoundException() {
        when(modelService.getModelById(MODEL_ID)).thenReturn(null);

        assertThrows(ModelNotFoundException.class, () -> service.buildChatModel(MODEL_ID, new JSONObject()));
    }

    @Test
    void buildChatModel_unknownProvider_throwsIllegalStateException() {
        when(modelService.getModelById(MODEL_ID)).thenReturn(model("UnknownProvider"));

        assertThrows(IllegalStateException.class, () -> service.buildChatModel(MODEL_ID, new JSONObject()));
    }

    @Test
    void buildChatModel_delegatesToProviderWithEmptyParamsWhenNull() {
        ModelEntity entity = model("OpenAI");
        when(modelService.getModelById(MODEL_ID)).thenReturn(entity);
        when(providerRegistry.get("OpenAI")).thenReturn(absProvider);
        ChatModel expected = mock(ChatModel.class);
        when(absProvider.buildChatModel(eq("gpt-4o"), eq(entity.getCredential()), eq(new JSONObject())))
                .thenReturn(expected);

        ChatModel actual = service.buildChatModel(MODEL_ID, null);

        assertSame(expected, actual);
        verify(absProvider).buildChatModel("gpt-4o", entity.getCredential(), new JSONObject());
    }

    @Test
    void buildScoringModel_delegatesWithEmptyParams() {
        ModelEntity entity = model("OpenAI");
        when(modelService.getModelById(MODEL_ID)).thenReturn(entity);
        when(providerRegistry.get("OpenAI")).thenReturn(absProvider);
        ScoringModel expected = mock(ScoringModel.class);
        when(absProvider.buildScoringModel(eq("gpt-4o"), eq(entity.getCredential()), eq(new JSONObject())))
                .thenReturn(expected);

        assertSame(expected, service.buildScoringModel(MODEL_ID));
    }

    private static ModelEntity model(String provider) {
        ModelEntity entity = new ModelEntity();
        entity.setId(MODEL_ID);
        entity.setProvider(provider);
        entity.setModelName("gpt-4o");
        ModelCredential credential = new ModelCredential();
        credential.setBaseUrl("https://api.openai.com/v1");
        credential.setApiKey("sk-test");
        entity.setCredential(credential);
        return entity;
    }
}
