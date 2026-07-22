package com.maxkb4j.model.registry;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.vo.ModelProviderInfo;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型供应商注册表：以 {@link ModelProviderType#provider()} 为键，集中持有所有供应商 Bean 及其元数据。
 *
 * <p>注册发生在启动期（{@link com.maxkb4j.model.processor.ModelProviderAutoRegistrar}），之后只读，
 * 使用 {@link LinkedHashMap} 保留声明顺序，替代原 {@code ModelProvider} 枚举的静态 {@code PROVIDER_MAP}。</p>
 */
@Getter
@Component
public class ModelProviderRegistry {

    private final Map<String, RegisteredProvider> providers = new LinkedHashMap<>();

    public void register(AbsModelProvider provider, ModelProviderType annotation) {
        providers.put(annotation.provider(),
                new RegisteredProvider(annotation.provider(), annotation.name(), annotation.icon(), provider));
    }

    /** 按供应商标识获取其实现，找不到返回 null。 */
    public AbsModelProvider get(String provider) {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }
        RegisteredProvider rp = providers.get(provider);
        return rp == null ? null : rp.provider();
    }

    public Collection<RegisteredProvider> all() {
        return providers.values();
    }

    /** 构建全部供应商的展示信息（替代 {@code ModelProvider.values()} + {@code getInfo()}）。 */
    public List<ModelProviderInfo> getProviderInfos() {
        return providers.values().stream()
                .map(rp -> new ModelProviderInfo(rp.key(), rp.name(), rp.icon()))
                .toList();
    }

    /** 一个已注册的供应商：标识 / 展示名 / 图标 / 实现 Bean。 */
    public record RegisteredProvider(String key, String name, String icon, AbsModelProvider provider) {
    }
}