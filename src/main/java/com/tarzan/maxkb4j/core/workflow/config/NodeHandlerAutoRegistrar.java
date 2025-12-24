package com.tarzan.maxkb4j.core.workflow.config;


import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.builder.NodeHandlerBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 自动扫描带有 @NodeHandlerType 注解的 INodeHandler Bean，并注册到 NodeHandlerBuilder
 */
@Slf4j
@Component
public class NodeHandlerAutoRegistrar implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (bean instanceof INodeHandler) {
            NodeHandlerType annotation = bean.getClass().getAnnotation(NodeHandlerType.class);
            if (annotation != null) {
                INodeHandler handler = (INodeHandler) bean;
                for (NodeType nodeType : annotation.value()) {
                    if (nodeType == null) {
                        log.warn("Skip empty node type in @NodeHandlerType on {}", bean.getClass().getName());
                        continue;
                    }
                    boolean replaced = NodeHandlerBuilder.registerHandler(nodeType, handler);
                    if (replaced) {
                        log.warn("Node handler for type '{}' was replaced by {}", nodeType, bean.getClass().getSimpleName());
                    } else {
                        log.debug("Registered node handler: {} -> {}", nodeType, bean.getClass().getSimpleName());
                    }
                }
            }
        }
        return bean;
    }
}