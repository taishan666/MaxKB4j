package com.tarzan.maxkb4j.listener;

import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssistantCompletedListener implements AiServiceCompletedListener {
    @Override
    public void onEvent(AiServiceCompletedEvent event) {
        Object result = event.result().orElse("");
        log.info(result.toString());
    }
}

