package com.maxkb4j.model.registry;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.provider.OpenAiModelProvider;
import com.maxkb4j.model.vo.ModelProviderInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ModelProviderRegistry} 注册/查找行为测试，使用真实的 {@link OpenAiModelProvider} 与其注解。
 */
class ModelProviderRegistryTest {

    private final ModelProviderRegistry registry = new ModelProviderRegistry();
    private final OpenAiModelProvider provider = new OpenAiModelProvider();

    @BeforeEach
    void setUp() {
        registry.register(provider, OpenAiModelProvider.class.getAnnotation(ModelProviderType.class));
    }

    @Test
    void get_returnsRegisteredProvider() {
        assertSame(provider, registry.get("OpenAI"));
    }

    @Test
    void get_nullOrEmpty_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> registry.get(null));
        assertThrows(IllegalArgumentException.class, () -> registry.get(""));
    }

    @Test
    void get_unknownProvider_returnsNull() {
        assertNull(registry.get("UnknownProvider"));
    }

    @Test
    void all_exposesRegisteredMetadata() {
        List<ModelProviderRegistry.RegisteredProvider> all = List.copyOf(registry.all());
        assertEquals(1, all.size());
        ModelProviderRegistry.RegisteredProvider rp = all.get(0);
        assertEquals("OpenAI", rp.key());
        assertEquals("OpenAI", rp.name());
        assertEquals("openai_icon.svg", rp.icon());
        assertSame(provider, rp.provider());
    }

    @Test
    void getProviderInfos_mapsRegisteredProviders() {
        List<ModelProviderInfo> infos = registry.getProviderInfos();
        assertEquals(1, infos.size());
        assertEquals("OpenAI", infos.get(0).getProvider());
        assertEquals("OpenAI", infos.get(0).getName());
        assertTrue(infos.get(0).getIcon() == null || infos.get(0).getIcon().contains("svg"));
    }
}
