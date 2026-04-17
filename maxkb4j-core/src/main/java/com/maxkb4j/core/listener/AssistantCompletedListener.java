package com.maxkb4j.core.listener;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import dev.langchain4j.service.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AssistantCompletedListener implements AiServiceCompletedListener {
    @Override
    public void onEvent(AiServiceCompletedEvent event) {
        Optional<Object> optional = event.result();
        optional.ifPresent(e -> {
            if (e instanceof Result<?> result) {
                ChatResponse chatResponse = result.finalResponse();
                log.info(chatResponse.toString());
            } else {
                log.info(e.toString());
            }
        });
    }
}

