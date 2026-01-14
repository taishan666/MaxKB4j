package com.tarzan.maxkb4j.listener;

import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssistantToolExecutedEventListener implements ToolExecutedEventListener {
    @Override
    public void onEvent(ToolExecutedEvent event) {
        log.info(event.request().toString());
        log.info(event.resultText());
    }
}
