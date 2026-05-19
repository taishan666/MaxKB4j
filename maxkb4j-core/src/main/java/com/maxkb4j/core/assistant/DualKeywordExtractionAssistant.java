package com.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DualKeywordExtractionAssistant {

    @UserMessage("""
        Given the following query, extract two types of keywords:
        1. High-level keywords: broad topics, themes, or concepts that describe the overall subject area (for searching relationship patterns)
        2. Low-level keywords: specific entities, names, or concrete details mentioned in the query (for searching specific entity nodes)
        Return as JSON with this exact format: {"high_level_keywords": ["keyword1", "keyword2"], "low_level_keywords": ["keyword1", "keyword2"]}
        Query: {{query}}
        It is very important that you return ONLY valid JSON and nothing else! \
        Do not include any explanations or additional text!""")
    Result<String> extractKeywords(@V("query") String query);
}