package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;


public interface Assistant {
    ChatResponse chat(String message);

    Flux<String> chatFlux(String message);

    TokenStream chatStream(String message);
}
