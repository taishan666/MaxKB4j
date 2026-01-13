package com.tarzan.maxkb4j.core.langchain4j;

import com.tarzan.maxkb4j.listener.AssistantCompletedListener;
import com.tarzan.maxkb4j.listener.AssistantErrorListener;
import com.tarzan.maxkb4j.listener.AssistantStartedListener;
import com.tarzan.maxkb4j.listener.AssistantToolExecutedEventListener;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;

import java.util.ArrayList;
import java.util.Collection;

public class AssistantServices {

    public static <T> AiServices<T> builder(Class<T> aiService) {
        return AiServices.builder(aiService).registerListeners(getListeners());
    }

    private static Collection<AiServiceListener<?>> getListeners(){
        Collection<AiServiceListener<?>> listeners = new ArrayList<>();
        listeners.add(new AssistantStartedListener());
        listeners.add(new AssistantCompletedListener());
        listeners.add(new AssistantToolExecutedEventListener());
        listeners.add(new AssistantErrorListener());
        return listeners;
    }

}
