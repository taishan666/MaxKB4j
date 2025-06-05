package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;


public interface Assistant {

    String chat(String message);

    Flux<String> chatFlux(String message);

    TokenStream chatStream(String message);

    TokenStream chatStream(ChatMessage chatMessage);


}
