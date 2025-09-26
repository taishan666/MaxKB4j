package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

import java.util.List;


public interface Assistant {

    Result<String> chat(String message);

    String chat(@UserMessage String userMessage, @UserMessage List<Content> contents);

    TokenStream chatStream(@UserMessage String userMessage);


    TokenStream chatStream(@UserMessage String userMessage, @UserMessage List<Content> contents);

    //将文本按照元素分行输出
    Result<List<String>>  generateOutlineFor(String message);

    @UserMessage("Is {{it}} a greeting statement?")
    boolean isGreeting(String text);


}
