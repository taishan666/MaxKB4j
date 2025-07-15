package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

import java.util.List;


public interface Assistant {

    Result<String> chat(String message);

    Result<String>  chat(UserMessage message);

    TokenStream chatStream(String message);

    TokenStream chatStream(UserMessage chatMessage);

    //将文本按照元素分行输出
    Result<List<String>>  generateOutlineFor(String message);

    @UserMessage("Is {{it}} a greeting statement?")
    boolean isGreeting(String text);


}
