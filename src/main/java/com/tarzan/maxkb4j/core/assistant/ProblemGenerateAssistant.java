package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.UserMessage;

import java.util.List;

public interface ProblemGenerateAssistant {

    List<String> generate(@UserMessage String userMessage);
}
