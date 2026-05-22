package com.maxkb4j.core.assistant;

import com.maxkb4j.common.domain.dto.DualKeywords;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;


public interface DualKeywordExtractionAssistant {

    @SystemMessage("Output in the language of Query")
    @UserMessage("""
        Given the following content, extract two types of keywords:
        1. High-level keywords: broad topics, themes, or concepts that describe the overall subject area (for searching relationship patterns)
        2. Low-level keywords: specific entities, names, or concrete details mentioned in the query (for searching specific entity nodes)
        Content: {{content}}
        It is very important that you return ONLY valid JSON and nothing else! \
        Do not include any explanations or additional text!""")
    Result<DualKeywords> extractKeywords(@V("content") String content);
}