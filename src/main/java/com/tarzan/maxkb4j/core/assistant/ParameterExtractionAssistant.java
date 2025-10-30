package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

public interface ParameterExtractionAssistant {

    @SystemMessage("""
        You are an information extraction assistant. Please accurately extract the information of the following fields from the user input:{{extractInfo}}
        If a certain piece of information is not mentioned or cannot be determined in the input, please set the corresponding field to null.
        """)
    Result<Map<String, Object>> extract(@V("extractInfo") String extractInfo, @UserMessage String userMessage);
}
