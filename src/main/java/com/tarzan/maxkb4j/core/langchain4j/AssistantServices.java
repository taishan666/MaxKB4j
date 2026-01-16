package com.tarzan.maxkb4j.core.langchain4j;

import com.tarzan.maxkb4j.listener.AssistantCompletedListener;
import com.tarzan.maxkb4j.listener.AssistantErrorListener;
import com.tarzan.maxkb4j.listener.AssistantStartedListener;
import com.tarzan.maxkb4j.listener.AssistantToolExecutedEventListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;

import java.util.Arrays;
import java.util.Collection;

public class AssistantServices {

    private static final Collection<AiServiceListener<?>> LISTENERS = Arrays.asList(
            new AssistantStartedListener(),
            new AssistantCompletedListener(),
            new AssistantToolExecutedEventListener(),
            new AssistantErrorListener()
    );

    public static <T> AiServices<T> builder(Class<T> aiService) {
        return AiServices.builder(aiService).registerListeners(LISTENERS);
    }

}
