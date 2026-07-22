package com.maxkb4j.model.annotation;

import java.lang.annotation.*;

/**
 * 标注一个 {@link com.maxkb4j.model.provider.AbsModelProvider} 实现为可被自动发现的模型供应商。
 *
 * <p>由 {@link com.maxkb4j.model.processor.ModelProviderAutoRegistrar} 在所有单例就绪后扫描，
 * 将带有本注解的 Bean 注册到 {@link com.maxkb4j.model.registry.ModelProviderRegistry}，
 * 从而替代原先 {@code ModelProvider} 枚举中硬编码的 {@code switch} 静态工厂。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModelProviderType {
    /** 供应商唯一标识，对应 {@code ModelEntity#getProvider()} 存储的值。 */
    String provider();
    /** 展示名称。 */
    String name();
    /** 图标文件名（位于 classpath:model-icons/ 下）。 */
    String icon();
}