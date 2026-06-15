package com.maxkb4j.workflow.processor;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.builder.CompareBuilder;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自动收集容器中所有 {@link Compare} 实现并按 {@link CompareType} 注解注册到
 * {@link CompareBuilder}。
 *
 * <p>使用 {@link SmartInitializingSingleton} 而非 {@code BeanPostProcessor}：
 * BPP 会在创建期触发 AspectJ AutoProxyCreator 枚举所有 {@code Advisor} Bean，
 * 进而强行提前实例化 Sa-Token 的 advisor，触发
 * "is not eligible for getting processed by all BeanPostProcessors" 警告。
 * 这里改为在所有单例就绪后统一注册，彻底避免该问题。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompareAutoRegistrar implements SmartInitializingSingleton {

    private final CompareBuilder compareBuilder;
    private final ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Compare> compareBeans = applicationContext.getBeansOfType(Compare.class);
        for (Map.Entry<String, Compare> entry : compareBeans.entrySet()) {
            Compare handler = entry.getValue();
            CompareType annotation = handler.getClass().getAnnotation(CompareType.class);
            if (annotation == null) {
                log.debug("Compare bean {} without @CompareType annotation will not be auto-registered",
                        handler.getClass().getName());
                continue;
            }
            CompareOperator[] operators = annotation.value();
            if (operators.length == 0) {
                log.warn("@CompareType on {} has no operators defined", handler.getClass().getName());
                continue;
            }
            boolean replaced = compareBuilder.registerHandler(operators, handler);
            if (replaced) {
                log.info("Compare handlers were replaced by {}", handler.getClass().getSimpleName());
            }
        }
    }
}
