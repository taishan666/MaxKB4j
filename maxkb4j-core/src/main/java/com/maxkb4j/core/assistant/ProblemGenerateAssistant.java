package com.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface ProblemGenerateAssistant {

    @SystemMessage("You are an expert at generating questions.")
    @UserMessage("""
             Generate {{n}} questions based on the content summary.
             These questions will be used to retrieve relevant documents.
             CRITICAL LANGUAGE REQUIREMENT:
             You MUST generate the questions in the EXACT SAME LANGUAGE as the provided 'Content'.\s
             Do not translate or switch languages.
             FORMATTING REQUIREMENT:
             It is very important to provide each question on a separate line,\s
             without enumerations, hyphens, bullet points, or any additional formatting!
             Content: {{content}}
            """)
    Result<List<String>> generate(@V("n") int n, @V("content")String  content);
}
