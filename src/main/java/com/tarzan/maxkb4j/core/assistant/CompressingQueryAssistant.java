package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CompressingQueryAssistant {

    @UserMessage("""
                    Read and understand the conversation between the User and the AI. \
                    Then, analyze the new query from the User. \
                    Identify all relevant details, terms, and context from both the conversation and the new query. \
                    Reformulate this query into a clear, concise, and self-contained format suitable for information retrieval.\
                    Conversation:
                    {{chatMemory}}
                    User query: {{query}}
                    It is very important that you provide only reformulated query and nothing else! \
                    Do not prepend a query with anything!""")
    Result<String> transform(@V("chatMemory") String chatMemory, @V("query")String  query);


}
