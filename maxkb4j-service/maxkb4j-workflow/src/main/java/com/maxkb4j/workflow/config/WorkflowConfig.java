package com.maxkb4j.workflow.config;

import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.registry.NodeCenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Workflow configuration: Spring bean wiring.
 *
 * <p>Node creators and handlers are now registered by annotation-driven auto-registrars
 * ({@link com.maxkb4j.workflow.processor.NodeCreatorAutoRegistrar} and
 * {@link com.maxkb4j.workflow.processor.NodeHandlerAutoRegistrar}) into the single
 * {@link NodeCenter} registry, so no manual bean wiring is required here.
 */
@Configuration
public class WorkflowConfig {

    /**
     * NodeBuilder bean: backed by {@link NodeCenter} instead of static lookups.
     */
    @Bean
    public NodeBuilder nodeBuilder(NodeCenter nodeCenter) {
        return new NodeBuilder(nodeCenter);
    }
}