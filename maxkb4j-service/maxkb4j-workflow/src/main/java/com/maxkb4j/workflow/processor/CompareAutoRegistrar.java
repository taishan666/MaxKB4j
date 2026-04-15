package com.maxkb4j.workflow.processor;


import com.maxkb4j.workflow.annotation.CompareType;
import com.maxkb4j.workflow.builder.CompareBuilder;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Auto-registers Compare beans annotated with @CompareType to CompareBuilder.
 * Refactored to inject CompareBuilder as Spring Bean for consistency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompareAutoRegistrar implements BeanPostProcessor {

    private final CompareBuilder compareBuilder;

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof Compare) {
            CompareType annotation = bean.getClass().getAnnotation(CompareType.class);
            if (annotation != null) {
                Compare handler = (Compare) bean;
                CompareOperator[] operators = annotation.value();
                if (operators.length == 0) {
                    log.warn("@CompareType on {} has no operators defined", bean.getClass().getName());
                    return bean;
                }
                boolean replaced = compareBuilder.registerHandler(operators, handler);
                if (replaced) {
                    log.info("Compare handlers were replaced by {}", bean.getClass().getSimpleName());
                }
            } else {
                log.debug("Compare bean {} without @CompareType annotation will not be auto-registered", bean.getClass().getName());
            }
        }
        return bean;
    }
}