package com.tarzan.maxkb4j.listener;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.listener.AiServiceStartedListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AssistantStartedListener implements AiServiceStartedListener {
    @Override
    public void onEvent(AiServiceStartedEvent event) {
        Optional<SystemMessage> systemMessage = event.systemMessage();
        UserMessage userMessage = event.userMessage();
        systemMessage.ifPresent(message -> log.info(message.toString()));
        log.info(userMessage.toString());
    }
}
