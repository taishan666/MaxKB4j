package com.maxkb4j.core.support;

import com.maxkb4j.common.domain.RagContent;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将检索命中的段落内容注入到用户问题中，拼装成带上下文的 Prompt。
 *
 * <p>通过 {@link RagContent} 最小契约与具体业务 VO 解耦，不依赖任何业务模块。
 */
public class RagContentInjector {
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("question:\n{{userMessage}}\nanswer using the following information:\n{{contents}}\nOutput Requirements:\nIf the knowledge base content contains image URLs, they must be output in the following Markdown format exactly as they are. ! [](image address)");
    private final PromptTemplate promptTemplate;

    public RagContentInjector() {
        this(DEFAULT_PROMPT_TEMPLATE);
    }

    public RagContentInjector(PromptTemplate promptTemplate) {
        this.promptTemplate = Utils.getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    }

    public String inject(List<? extends RagContent> contents, String problemText) {
        if (contents == null || contents.isEmpty()) {
            return problemText;
        }
        return this.createPrompt(problemText, contents).text();
    }

    public String inject(List<? extends RagContent> contents, String problemText, int maxCharNumber) {
        if (contents == null || contents.isEmpty()) {
            return problemText;
        }
        return this.createPrompt(problemText, contents, maxCharNumber).text();
    }

    private Prompt createPrompt(String problemText, List<? extends RagContent> contents) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.format(contents));
        return this.promptTemplate.apply(variables);
    }

    private Prompt createPrompt(String problemText, List<? extends RagContent> contents, int maxCharNumber) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.format(contents, maxCharNumber));
        return this.promptTemplate.apply(variables);
    }

    private String format(List<? extends RagContent> contents) {
        return contents.stream().map(this::formatContent).collect(Collectors.joining("\n\n"));
    }

    public String format(List<? extends RagContent> contents, int maxCharNumber) {
        String data = format(contents);
        if (data.length() > maxCharNumber) {
            return data.substring(0, maxCharNumber);
        }
        return data;
    }

    private String formatContent(RagContent content) {
        String title = content.getTitle();
        String body = content.getContent();
        // title 为 null/空时直接返回正文，避免 title.isEmpty() 抛 NPE
        return StringUtils.isEmpty(title) ? body : String.format("content: %s\n%s", title, body);
    }

}
