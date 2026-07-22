package com.maxkb4j.model.processor;

import com.maxkb4j.model.annotation.ModelProviderType;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自动扫描带有 {@link ModelProviderType} 注解的 {@link AbsModelProvider} Bean，
 * 并注册到 {@link ModelProviderRegistry}。
 *
 * <p>使用 {@link SmartInitializingSingleton} 而非 {@code BeanPostProcessor}：
 * 与工作流节点处理器的注册保持一致，避免在创建期提前触发其它 BeanPostProcessor 的副作用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelProviderAutoRegistrar implements SmartInitializingSingleton {

    private final ModelProviderRegistry registry;
    private final ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, AbsModelProvider> beans = applicationContext.getBeansOfType(AbsModelProvider.class);
        for (AbsModelProvider provider : beans.values()) {
            ModelProviderType annotation = provider.getClass().getAnnotation(ModelProviderType.class);
            if (annotation == null) {
                log.warn("Skip AbsModelProvider bean without @ModelProviderType: {}", provider.getClass().getName());
                continue;
            }
            registry.register(provider, annotation);
        }
        log.info("ModelProviderRegistry initialized with {} providers", registry.all().size());
    }
}