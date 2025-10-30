package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface ExpandingQueryAssistant {

    @UserMessage("Generate {{n}} different versions of a provided user query. Each version should be worded differently, using synonyms or alternative sentence structures, but they should all retain the original meaning. These versions will be used to retrieve relevant documents. It is very important to provide each query version on a separate line, without enumerations, hyphens, or any additional formatting!\nUser query: {{query}}")
    Result<List<String>> transform(@V("n") int n, @V("query")String  query);

}
