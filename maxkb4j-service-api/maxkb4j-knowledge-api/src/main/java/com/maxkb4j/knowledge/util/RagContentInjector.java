package com.maxkb4j.knowledge.util;

import com.maxkb4j.knowledge.vo.ParagraphVO;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RagContentInjector {
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("{{userMessage}}\n\nAnswer using the following information:\n{{contents}}");
    private final PromptTemplate promptTemplate;

    public RagContentInjector() {
        this(DEFAULT_PROMPT_TEMPLATE);
    }


    public RagContentInjector(PromptTemplate promptTemplate) {
        this.promptTemplate = Utils.getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    }

    public static RagContentInjector.RagContentInjectorBuilder builder() {
        return new RagContentInjector.RagContentInjectorBuilder();
    }

    public String inject(List<ParagraphVO> contents, String problemText) {
        if (contents.isEmpty()) {
            return problemText;
        } else {
            Prompt prompt = this.createPrompt(problemText, contents);
            return prompt.text();
        }
    }

    public String inject(List<ParagraphVO> contents, String problemText, int maxCharNumber) {
        if (contents.isEmpty()) {
            return problemText;
        } else {
            Prompt prompt = this.createPrompt(problemText, contents, maxCharNumber);
            return prompt.text();
        }
    }

    protected Prompt createPrompt(String problemText, List<ParagraphVO> contents) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.format(contents));
        return this.promptTemplate.apply(variables);
    }

    protected Prompt createPrompt(String problemText, List<ParagraphVO> contents, int maxCharNumber) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.format(contents, maxCharNumber));
        return this.promptTemplate.apply(variables);
    }

    protected String format(List<ParagraphVO> contents) {
        return contents.stream().map(this::format).collect(Collectors.joining("\n\n"));
    }

    public String format(List<ParagraphVO> contents, int maxCharNumber) {
        String data = contents.stream().map(this::format).collect(Collectors.joining("\n\n"));
        if (data.length() > maxCharNumber) {
            return data.substring(0, maxCharNumber);
        }
        return data;
    }

    protected String format(ParagraphVO content) {
        return this.format(content.getTitle(), content.getContent());
    }

    protected String format(String segmentContent, String segmentMetadata) {
        return segmentMetadata.isEmpty() ? segmentContent : String.format("content: %s\n%s", segmentContent, segmentMetadata);
    }

    public static class RagContentInjectorBuilder {
        private PromptTemplate promptTemplate;
        private List<String> metadataKeysToInclude;

        RagContentInjectorBuilder() {
        }

        public RagContentInjector.RagContentInjectorBuilder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public RagContentInjector build() {
            return new RagContentInjector(this.promptTemplate);
        }
    }
}
