package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.*;

import java.util.List;


public interface Assistant {

    Result<String> chat(String message);


    TokenStream chatStream(String message);

    //将文本按照元素分行输出
    Result<List<String>>  generateOutlineFor(String message);

    @UserMessage("Is {{it}} a greeting statement?")
    boolean isGreeting(String text);


}
