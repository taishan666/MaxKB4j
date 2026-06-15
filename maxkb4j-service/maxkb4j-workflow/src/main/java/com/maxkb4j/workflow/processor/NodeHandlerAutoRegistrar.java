package com.maxkb4j.workflow.processor;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.registry.NodeHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自动扫描带有 {@link NodeHandlerType} 注解的 {@link INodeHandler} Bean，
 * 并注册到 {@link NodeHandlerRegistry}。
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
public class NodeHandlerAutoRegistrar implements SmartInitializingSingleton {

    private final NodeHandlerRegistry handlerRegistry;
    private final ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, INodeHandler> handlerBeans = applicationContext.getBeansOfType(INodeHandler.class);
        for (Map.Entry<String, INodeHandler> entry : handlerBeans.entrySet()) {
            INodeHandler handler = entry.getValue();
            NodeHandlerType annotation = handler.getClass().getAnnotation(NodeHandlerType.class);
            if (annotation == null) {
                continue;
            }
            for (NodeType nodeType : annotation.value()) {
                if (nodeType == null) {
                    log.warn("Skip empty node type in @NodeHandlerType on {}", handler.getClass().getName());
                    continue;
                }
                handlerRegistry.register(nodeType.getKey(), handler);
            }
        }
    }
}
