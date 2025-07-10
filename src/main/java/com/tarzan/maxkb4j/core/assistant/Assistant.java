package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;

import java.util.List;


public interface Assistant {

    ChatResponse chat(String message);

    ChatResponse chat(UserMessage message);

    TokenStream chatStream(String message);

    TokenStream chatStream(UserMessage chatMessage);

    Result<List<String>>  generate(String message);


}
