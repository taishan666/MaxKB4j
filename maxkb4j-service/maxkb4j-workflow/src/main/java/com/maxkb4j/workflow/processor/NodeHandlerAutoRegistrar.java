package com.maxkb4j.workflow.processor;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.registry.NodeCenter;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 自动扫描带有 @NodeHandlerType 注解的 INodeHandler Bean，并注册到 NodeCenter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeHandlerAutoRegistrar implements BeanPostProcessor {

    private final NodeCenter nodeCenter;

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof INodeHandler handler) {
            NodeHandlerType annotation = bean.getClass().getAnnotation(NodeHandlerType.class);
            if (annotation != null) {
                for (NodeType nodeType : annotation.value()) {
                    if (nodeType == null) {
                        log.warn("Skip empty node type in @NodeHandlerType on {}", bean.getClass().getName());
                        continue;
                    }
                    nodeCenter.registerHandler(nodeType.getKey(), handler);
                }
            }
        }
        return bean;
    }
}