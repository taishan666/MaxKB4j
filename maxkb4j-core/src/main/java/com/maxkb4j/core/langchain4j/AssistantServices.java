package com.maxkb4j.core.langchain4j;

import com.maxkb4j.core.listener.AssistantCompletedListener;
import com.maxkb4j.core.listener.AssistantErrorListener;
import com.maxkb4j.core.listener.AssistantStartedListener;
import com.maxkb4j.core.listener.AssistantToolExecutedEventListener;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class AssistantServices {

    private static final Collection<AiServiceListener<?>> LISTENERS = Arrays.asList(
            new AssistantStartedListener(),
            new AssistantCompletedListener(),
            new AssistantToolExecutedEventListener(),
            new AssistantErrorListener()
    );

    public static <T> AiServices<T> builder(Class<T> aiService) {
        Function<ToolExecutionRequest, ToolExecutionResultMessage> strategy = (request) -> ToolExecutionResultMessage.builder()
                .id(request.id())
                .toolName(request.name())
                .text("工具（"+request.name()+"）不存在！")
                .isError(true)
                .build();
        return AiServices.builder(aiService)
                .storeRetrievedContentInChatMemory(false)
                .hallucinatedToolNameStrategy(strategy)
                .registerListeners(LISTENERS);
    }

}
