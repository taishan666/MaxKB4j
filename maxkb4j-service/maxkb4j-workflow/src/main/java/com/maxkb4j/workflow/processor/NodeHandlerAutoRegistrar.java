package com.maxkb4j.workflow.processor;

import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.registry.NodeCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Auto-scans {@link INodeHandler} beans carrying {@link NodeHandlerType} and registers them
 * into {@link NodeCenter}.
 *
 * <p>Uses {@link SmartInitializingSingleton} rather than a {@code BeanPostProcessor}: a BPP
 * would, during its creation phase, force eager instantiation of the AspectJ AutoProxyCreator
 * (which abstracts all {@code Advisor} beans) and in turn eagerly instantiate Sa-Token's
 * advisor, triggering the "is not eligible for getting processed by all BeanPostProcessors"
 * warning. Registering here, after all singletons are ready, fully avoids that.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeHandlerAutoRegistrar implements SmartInitializingSingleton {

    private final NodeCenter nodeCenter;
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
                nodeCenter.registerHandler(nodeType.getKey(), handler);
            }
        }
    }
}