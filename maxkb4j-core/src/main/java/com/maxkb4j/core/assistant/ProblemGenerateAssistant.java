package com.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface ProblemGenerateAssistant {

    //@SystemMessage("You are an expert at generating questions.")
    @UserMessage("""
            Generate {{n}} questions based on the content summary.
            These questions will be used to retrieve relevant documents. \
            It is very important to provide each question on a separate line, without enumerations, hyphens, or any additional formatting!\
            Content: {{content}}
            """)
    Result<List<String>> generate(@V("n") int n, @V("content")String  content);
}
