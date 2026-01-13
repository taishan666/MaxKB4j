package com.tarzan.maxkb4j.listener;

import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssistantErrorListener implements AiServiceErrorListener {
    @Override
    public void onEvent(AiServiceErrorEvent event) {
        Throwable error = event.error();
        log.error(error.getMessage());
    }
}
