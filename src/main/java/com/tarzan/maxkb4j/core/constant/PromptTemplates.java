package com.tarzan.maxkb4j.core.constant;

import dev.langchain4j.model.input.PromptTemplate;

public interface PromptTemplates {

    PromptTemplate RAG_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                    {{userMessage}}
                    
                    Use the contents of the <Data></Data> tag as your knowledge:
                    <Data>
                     {{contents}}
                    </Data>
                    Answer the request:
                    
                    - If you don't know, just say you don't know. If you don't know when in doubt, seek clarification.
                    
                    - Avoid mentioning information that you get from context.
                    
                    - Answer in the same language as the question.
                    """);
}
