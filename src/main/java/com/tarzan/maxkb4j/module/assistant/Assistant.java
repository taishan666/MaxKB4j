package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

import java.util.List;


public interface Assistant {
    String chat(String message);

    Flux<String> chatFlux(String message);

    TokenStream chatStream(Query  query);

    TokenStream chatStream(String message);

    TokenStream chatStream(ChatMessage chatMessage);

    TokenStream chatStream(List<ChatMessage> messages);

}
