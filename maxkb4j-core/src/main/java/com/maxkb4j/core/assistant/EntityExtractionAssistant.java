package com.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EntityExtractionAssistant {

    @UserMessage("""
        From the following text, extract all named entities and relationships between them.
        Return the result as a JSON array of objects with the following format:
        - For entities: {"type": "entity", "name": "entity_name", "entity_type": "person/organization/concept/etc", "description": "brief description of this entity"}
        - For relationships: {"type": "relationship", "source_entity": "source_entity_name", "target_entity": "target_entity_name", "description": "relationship description", "keywords": "comma-separated topic keywords"}
        Text: {{text}}
        It is very important that you return ONLY a valid JSON array and nothing else! \
        Do not include any explanations, headers, or formatting outside the JSON array!""")
    Result<String> extract(@V("text") String text);
}