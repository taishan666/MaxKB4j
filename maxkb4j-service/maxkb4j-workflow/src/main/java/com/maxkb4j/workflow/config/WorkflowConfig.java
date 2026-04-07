package com.maxkb4j.workflow.config;

import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.factory.NodeRegistry;
import com.maxkb4j.workflow.registry.NodeCenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流配置类
 * 提供 Spring Bean 定义
 */
@Configuration
public class WorkflowConfig {

    /**
     * 节点注册表 Bean
     * 作为普通类被 NodeCenter 使用
     */
    @Bean
    public NodeRegistry nodeRegistry() {
        return new NodeRegistry();
    }

    /**
     * NodeBuilder Bean
     * 通过注入 NodeCenter 替代静态调用
     */
    @Bean
    public NodeBuilder nodeBuilder(NodeCenter nodeCenter) {
        return new NodeBuilder(nodeCenter);
    }

}