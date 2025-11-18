package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;


public interface Assistant {

    Result<String> chat(@UserMessage String userMessage);

    Result<String> chat(@UserMessage String userMessage, @UserMessage List<Content> contents);

    TokenStream chatStream(@UserMessage String userMessage);

    TokenStream chatStream(@UserMessage String userMessage, @UserMessage List<Content> contents);

    @UserMessage("Is {{it}} a greeting statement?")
    boolean isGreeting(@V("it") String text);


}
