package com.maxkb4j.model.provider;

import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.vo.ModelInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.ModelName;
import static com.maxkb4j.model.consts.ModelConstants.ParamKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiModelProviderTest {

    private final OpenAiModelProvider provider = new OpenAiModelProvider();

    @Test
    void getDefaultBaseUrl_returnsOpenAiEndpoint() {
        assertEquals(BaseUrl.OPENAI, provider.getDefaultBaseUrl());
    }

    @Test
    void getBaseUrl_blankFallsBackToDefault() {
        assertEquals(BaseUrl.OPENAI, provider.getBaseUrl(null));
        assertEquals(BaseUrl.OPENAI, provider.getBaseUrl("  "));
        assertEquals("https://custom.example.com/v1", provider.getBaseUrl("https://custom.example.com/v1"));
    }

    @Test
    void getModelInfo_resolvesByTypeAndName() {
        ModelInfo gpt4o = provider.getModelInfo(ModelType.LLM, ModelName.GPT_4O);
        assertEquals("GPT-4 Omni", gpt4o.getDesc());
        assertNull(provider.getModelInfo(ModelType.EMBEDDING, ModelName.GPT_4O));
    }

    @Test
    void getModelParamsForm_chatFormHasTemperatureAndMaxTokens() {
        List<String> fields = provider.getModelParamsForm(ModelType.LLM.getKey())
                .stream().map(f -> f.getField()).toList();
        assertTrue(fields.contains(ParamKey.TEMPERATURE));
        assertTrue(fields.contains(ParamKey.MAX_TOKENS));
    }
}
