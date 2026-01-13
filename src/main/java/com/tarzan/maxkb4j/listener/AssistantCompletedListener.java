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
        // The invocationId will be the same for all events related to the same LLM invocation
      /*  InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        Object chatMemoryId = invocationContext.chatMemoryId();
        Instant eventTimestamp = invocationContext.timestamp();
        System.out.println("invocationId="+invocationId);
        System.out.println("chatMemoryId="+chatMemoryId);*/
    }
}

