package com.maxkb4j.workflow.processor;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.registry.NodeCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * Auto-scans classes annotated with {@link NodeCreatorType} and registers a reflective
 * creator for each into {@link NodeCenter}, replacing the hand-written register table that
 * previously lived in {@code NodeCenter#registerDefaultNodeCreators}.
 *
 * <p>Node implementations are plain POJOs (not Spring beans), so the scan uses
 * {@link ClassPathScanningCandidateComponentProvider} rather than {@code getBeansOfType},
 * keeping the registration mechanism otherwise identical to
 * {@link NodeHandlerAutoRegistrar}.
 *
 * <p>Like the handler registrar it runs as {@link SmartInitializingSingleton} (after all
 * singletons are created) so that no early BeanPostProcessor side effects are triggered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeCreatorAutoRegistrar implements SmartInitializingSingleton {

    private static final String NODE_PACKAGE = "com.maxkb4j.workflow.node.impl";

    private final NodeCenter nodeCenter;

    @Override
    public void afterSingletonsInstantiated() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(NodeCreatorType.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(NODE_PACKAGE);
        for (BeanDefinition candidate : candidates) {
            String className = candidate.getBeanClassName();
            try {
                Class<?> clazz = Class.forName(className);
                NodeCreatorType annotation = clazz.getAnnotation(NodeCreatorType.class);
                if (annotation == null) {
                    continue;
                }
                NodeType nodeType = annotation.value();
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, JSONObject.class);
                constructor.setAccessible(true);
                nodeCenter.registerCreator(nodeType.getKey(), (id, properties) -> {
                    try {
                        return constructor.newInstance(id, properties);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("Failed to instantiate node " + clazz.getSimpleName(), e);
                    }
                });
                log.debug("Registered node creator {} -> {}", nodeType.getKey(), clazz.getSimpleName());
            } catch (Exception e) {
                log.error("Failed to register node creator: {}", className, e);
            }
        }
        log.info("NodeCenter initialized with {} node creators", nodeCenter.creatorCount());
    }
}