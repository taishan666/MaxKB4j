package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

import java.util.List;

public interface Assistant {
    ChatResponse chat(List<ChatMessage> messages);

    Flux<String> chatFlux(List<ChatMessage> messages);

    TokenStream chatStream(List<ChatMessage> messages);
}
