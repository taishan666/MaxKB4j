package com.maxkb4j.tool.processor;

import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.provider.AbsToolHandler;
import com.maxkb4j.tool.registry.ToolHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Auto-scans {@link AbsToolHandler} beans annotated with {@link ToolHandlerType} and
 * registers them into {@link ToolHandlerRegistry}. Uses
 * {@link SmartInitializingSingleton} (consistent with ModelProviderAutoRegistrar) so
 * registration runs after all singletons are created, avoiding early side effects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolHandlerAutoRegistrar implements SmartInitializingSingleton {

    private final ToolHandlerRegistry registry;
    private final ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, AbsToolHandler> beans = applicationContext.getBeansOfType(AbsToolHandler.class);
        for (AbsToolHandler handler : beans.values()) {
            ToolHandlerType annotation = handler.getClass().getAnnotation(ToolHandlerType.class);
            if (annotation == null) {
                log.warn("Skip AbsToolHandler bean without @ToolHandlerType: {}", handler.getClass().getName());
                continue;
            }
            registry.register(handler, annotation);
        }
        log.info("ToolHandlerRegistry initialized with {} handlers", registry.getHandlers().size());
    }
}