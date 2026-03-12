package com.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface RouterAssistant {

    @UserMessage("Based on the user query, determine the most suitable data source(s) to retrieve relevant information from the following options:\n{{options}}\nIt is very important that your answer consists of either a single number or multiple numbers separated by commas and nothing else!\nUser query: {{query}}")
    Result<List<String>> route(@V("options") String options, @V("query")String  query);

}
