package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IntentClassifyAssistant {

    String SYSTEM_MESSAGE = "You are a professional intent recognition assistant. Please accurately identify the user's true intent based on the user's input and intent options.";

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage("""
            Based on the user query, \
            determine the most suitable option(s) to retrieve relevant information from the following options:
            {{options}}
            It is very important that your answer consists of a single number and nothing else!
            Conversation: {{chatMemory}}
            User query: {{query}}""")
    Result<String> route(@V("options") String options,@V("chatMemory") String chatMemory, @V("query")String  query);


    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage("""
            Based on the user query, \
            determine the most suitable option(s) to retrieve relevant information from the following options:
            {{options}}
            It is very important that your answer consists of a single number and nothing else!
            Background information: {{background}}
            Conversation: {{chatMemory}}
            User query: {{query}}""")
    Result<String> route(@V("options") String options,@V("chatMemory") String chatMemory, @V("background")String  background, @V("query")String  query);
}
