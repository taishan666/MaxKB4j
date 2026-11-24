package com.maxkb4j.core.listener;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AssistantToolExecutedEventListener implements ToolExecutedEventListener {
    @Override
    public void onEvent(ToolExecutedEvent event) {
        ToolExecutionRequest request = event.request();
        if (request != null) {
            log.info(request.toString());
        }
        log.info(event.resultText());
    }
}
