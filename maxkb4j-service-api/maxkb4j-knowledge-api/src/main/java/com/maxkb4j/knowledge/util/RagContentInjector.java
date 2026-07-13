package com.maxkb4j.knowledge.util;

import com.maxkb4j.knowledge.vo.ParagraphVO;
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
 */
public class RagContentInjector {
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("{{userMessage}}\n\nAnswer using the following information:\n{{contents}}");
    private final PromptTemplate promptTemplate;

    public RagContentInjector() {
        this(DEFAULT_PROMPT_TEMPLATE);
    }

    public RagContentInjector(PromptTemplate promptTemplate) {
        this.promptTemplate = Utils.getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    }

    public String inject(List<ParagraphVO> contents, String problemText) {
        if (contents == null || contents.isEmpty()) {
            return problemText;
        }
        return this.createPrompt(problemText, contents).text();
    }

    public String inject(List<ParagraphVO> contents, String problemText, int maxCharNumber) {
        if (contents == null || contents.isEmpty()) {
            return problemText;
        }
        return this.createPrompt(problemText, contents, maxCharNumber).text();
    }

    private Prompt createPrompt(String problemText, List<ParagraphVO> contents) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.format(contents));
        return this.promptTemplate.apply(variables);
    }

    private Prompt createPrompt(String problemText, List<ParagraphVO> contents, int maxCharNumber) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.format(contents, maxCharNumber));
        return this.promptTemplate.apply(variables);
    }

    private String format(List<ParagraphVO> contents) {
        return contents.stream().map(this::formatParagraph).collect(Collectors.joining("\n\n"));
    }

    public String format(List<ParagraphVO> contents, int maxCharNumber) {
        String data = format(contents);
        if (data.length() > maxCharNumber) {
            return data.substring(0, maxCharNumber);
        }
        return data;
    }

    private String formatParagraph(ParagraphVO paragraph) {
        String title = paragraph.getTitle();
        String content = paragraph.getContent();
        // title 为 null/空时直接返回 content，避免 title.isEmpty() 抛 NPE
        return StringUtils.isEmpty(title) ? content : String.format("content: %s\n%s", title, content);
    }

}
