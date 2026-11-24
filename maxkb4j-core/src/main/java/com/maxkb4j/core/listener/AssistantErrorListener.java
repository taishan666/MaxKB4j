package com.maxkb4j.core.listener;

import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssistantErrorListener implements AiServiceErrorListener {
    @Override
    public void onEvent(AiServiceErrorEvent event) {
        Throwable error = event.error();
        if (error == null) {
            log.error("AiService error event received without a cause");
            return;
        }
        log.error("AiService error: {}", error.getMessage(), error);
    }
}
