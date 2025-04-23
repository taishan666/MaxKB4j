package com.tarzan.maxkb4j.module.rag;

import dev.langchain4j.internal.Utils;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MyQueryClassifier {
    protected final String options;
    protected final Map<Integer, String> idToClassification;
    protected final PromptTemplate promptTemplate;
    protected final ChatLanguageModel chatLanguageModel;
    public static final String DEFAULT_PROMPT_TEMPLATE = """
            Based on the user query, \
            determine the most suitable data source(s) to retrieve relevant information from the following options:
            {{options}}
            It is very important that your answer consists of a single number and nothing else!
            User query: {{query}}""";


    public MyQueryClassifier(ChatLanguageModel chatLanguageModel, Map<String, String> classificationDescription) {
        this(chatLanguageModel, classificationDescription, PromptTemplate.from(DEFAULT_PROMPT_TEMPLATE));
    }

    public MyQueryClassifier(ChatLanguageModel chatLanguageModel, Map<String, String> classificationDescription, PromptTemplate promptTemplate) {
        this.chatLanguageModel = ValidationUtils.ensureNotNull(chatLanguageModel, "chatLanguageModel");
        ValidationUtils.ensureNotEmpty(classificationDescription, "retrieverToDescription");
        this.promptTemplate = Utils.getOrDefault(promptTemplate, PromptTemplate.from(DEFAULT_PROMPT_TEMPLATE));
        Map<Integer, String> idToClassification = new HashMap<>();
        StringBuilder optionsBuilder = new StringBuilder();
        int id = 1;

        for(Iterator<Map.Entry<String, String>> var8 = classificationDescription.entrySet().iterator(); var8.hasNext(); ++id) {
            Map.Entry entry = var8.next();
            idToClassification.put(id, ValidationUtils.ensureNotNull(entry.getKey(), "ContentRetriever").toString());
            if (id > 1) {
                optionsBuilder.append("\n");
            }

            optionsBuilder.append(id);
            optionsBuilder.append(": ");
            optionsBuilder.append(ValidationUtils.ensureNotBlank((String) entry.getValue(), "ContentRetriever description"));
        }

        this.idToClassification = idToClassification;
        this.options = optionsBuilder.toString();
    }


    public Collection<String> route(Query query) {
        Prompt prompt = this.createPrompt(query);
        try {
            String response = this.chatLanguageModel.chat(prompt.text());
            return this.parse(response);
        } catch (Exception var4) {
            log.warn("Failed to route query '{}'", query.text(), var4);
            return Collections.emptyList();
        }
    }

    protected Collection<String> parse(String choices) {
        Stream<Integer> classificationIds = Arrays.stream(choices.split(",")).map(String::trim).map(Integer::parseInt);
        Map<Integer, String> map = this.idToClassification;
        Objects.requireNonNull(map);
        return classificationIds.map(map::get).collect(Collectors.toList());
    }


    protected Prompt createPrompt(Query query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query.text());
        variables.put("options", this.options);
        return this.promptTemplate.apply(variables);
    }
}
