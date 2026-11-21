package com.maxkb4j.model.provider;

import org.junit.jupiter.api.Test;

import static com.maxkb4j.model.consts.ModelConstants.BaseUrl;
import static com.maxkb4j.model.consts.ModelConstants.ModelName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AliYunBaiLianModelProvider} 的 baseUrl 归一化逻辑测试。
 *
 * <p>DashScope 原生 SDK 拼接 {@code /services/...} 路径时必须使用 {@code /api/v1} 形式，
 * 兼容模式地址（{@code /compatible-mode/v1}）直接传入会 404。</p>
 */
class AliYunBaiLianModelProviderTest {

    private final AliYunBaiLianModelProvider provider = new AliYunBaiLianModelProvider();

    @Test
    void getDashScopeBaseUrl_nullFallsBackToDefaultNativeUrl() {
        assertEquals(BaseUrl.ALI_YUN_BAI_LIAN_NATIVE, provider.getDashScopeBaseUrl(null));
        assertEquals(BaseUrl.ALI_YUN_BAI_LIAN_NATIVE, provider.getDashScopeBaseUrl("  "));
    }

    @Test
    void getDashScopeBaseUrl_convertsCompatibleModeToNative() {
        assertEquals(BaseUrl.ALI_YUN_BAI_LIAN_NATIVE,
                provider.getDashScopeBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1"));
    }

    @Test
    void getDashScopeBaseUrl_trimsTrailingSlashBeforeConverting() {
        assertEquals(BaseUrl.ALI_YUN_BAI_LIAN_NATIVE,
                provider.getDashScopeBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/"));
        assertEquals(BaseUrl.ALI_YUN_BAI_LIAN_NATIVE,
                provider.getDashScopeBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1///"));
    }

    @Test
    void getDashScopeBaseUrl_nativeUrlAndCustomUrlKeptAsIs() {
        assertEquals(BaseUrl.ALI_YUN_BAI_LIAN_NATIVE,
                provider.getDashScopeBaseUrl("https://dashscope.aliyuncs.com/api/v1"));
        assertEquals("https://custom.example.com/v1",
                provider.getDashScopeBaseUrl("https://custom.example.com/v1"));
    }

    @Test
    void supportsAllBaiLianCapabilities() {
        assertTrue(provider.isSupport(com.maxkb4j.model.enums.ModelType.LLM));
        assertTrue(provider.isSupport(com.maxkb4j.model.enums.ModelType.EMBEDDING));
        assertTrue(provider.isSupport(com.maxkb4j.model.enums.ModelType.RERANKER));
        assertTrue(provider.isSupport(com.maxkb4j.model.enums.ModelType.TTI));
    }

    @Test
    void embeddingModelInfoResolvable() {
        assertTrue(provider.getModelList().stream()
                .anyMatch(info -> ModelName.TEXT_EMBEDDING_V3.equals(info.getName())));
    }
}
