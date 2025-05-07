package com.tarzan.maxkb4j.listener;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.spring.event.AiServiceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class AiServiceRegisteredEventListener implements ApplicationListener<AiServiceRegisteredEvent> {


    @Override
    public void onApplicationEvent(AiServiceRegisteredEvent event) {
        Class<?> aiServiceClass = event.aiServiceClass();
        List<ToolSpecification> toolSpecifications = event.toolSpecifications();
        for (int i = 0; i < toolSpecifications.size(); i++) {
            System.out.printf("[%s]: [Tool-%s]: %s%n", aiServiceClass.getSimpleName(), i + 1, toolSpecifications.get(i));
        }

    }
}