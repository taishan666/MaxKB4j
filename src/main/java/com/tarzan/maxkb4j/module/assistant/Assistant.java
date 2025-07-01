package com.tarzan.maxkb4j.module.assistant;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;


public interface Assistant {

    ChatResponse chat(String message);

    ChatResponse chat(UserMessage message);

    TokenStream chatStream(String message);

    TokenStream chatStream(UserMessage chatMessage);


}
