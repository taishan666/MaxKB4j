package com.maxkb4j.core.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.vo.RagContent;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 将检索命中的段落内容注入到用户问题中，拼装成带上下文的 Prompt。
 *
 * <p>通过 {@link RagContent} 最小契约与具体业务 VO 解耦，不依赖任何业务模块。</p>
 */
public class RagContentInjector {

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("""
            Answer the question based on the numbered knowledge base contents below.
            question:
            {{userMessage}}
            answer using the following json information:
            {{contents}}
            Output Requirements:
            1. Answer ONLY using the provided contents. If they are insufficient to answer the question, state that explicitly instead of inventing information.
            2. Cite the reference ids (e.g. [1] [2]) next to the corresponding statements in your answer.
            3. If the knowledge base content contains image URLs, they must be output in the following Markdown format exactly as they are. ! [](image address)""");

    private final PromptTemplate promptTemplate;

    public RagContentInjector() {
        this(DEFAULT_PROMPT_TEMPLATE);
    }

    public RagContentInjector(PromptTemplate promptTemplate) {
        this.promptTemplate = Utils.getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    }

    public String inject(List<? extends RagContent> contents, String problemText, int maxCharNumber) {
        if (contents == null || contents.isEmpty()) {
            return problemText;
        }
        return this.createPrompt(problemText, contents, maxCharNumber).text();
    }

    private Prompt createPrompt(String problemText, List<? extends RagContent> contents, int maxCharNumber) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", problemText);
        variables.put("contents", this.formatJson(contents, maxCharNumber));
        return this.promptTemplate.apply(variables);
    }

    /**
     * 将命中内容序列化为带引用编号的紧凑 JSON：
     * 按文档分组（保持命中顺序）、组内按 position 升序、每条分配全局递增的 id 供答案引用。
     *
     * <p>预算（maxCharNumber）控制 title+content 的总字符数：放不下整条时对最后一条做
     * 字符级截断而非整块丢弃，提高预算利用率；紧凑序列化（无缩进）较 PrettyFormat
     * 节省约 30% 的无效 token。</p>
     */
    public String formatJson(List<? extends RagContent> contents, int maxCharNumber) {
        Map<String, List<RagContent>> byDocument = new LinkedHashMap<>();
        for (RagContent content : contents) {
            byDocument.computeIfAbsent(StringUtils.defaultString(content.getDocumentName()), k -> new ArrayList<>()).add(content);
        }

        JSONArray groups = new JSONArray();
        int budget = maxCharNumber;
        int citation = 0;
        boolean exhausted = budget <= 0;
        for (Map.Entry<String, List<RagContent>> entry : byDocument.entrySet()) {
            if (exhausted) {
                break;
            }
            JSONArray contentArray = new JSONArray();
            List<RagContent> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparing(RagContent::getPosition, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (RagContent content : sorted) {
                if (budget <= 0) {
                    exhausted = true;
                    break;
                }
                String title = StringUtils.defaultString(content.getTitle());
                String body = StringUtils.defaultString(content.getContent());
                int len = title.length() + body.length();
                if (len > budget) {
                    // 预算不足：title 优先保留，正文截断到剩余预算后停止
                    int bodyBudget = budget - title.length();
                    if (bodyBudget > 0) {
                        body = body.substring(0, bodyBudget);
                    } else {
                        title = title.substring(0, budget);
                        body = "";
                    }
                    len = budget;
                }
                budget -= len;
                JSONObject obj = new JSONObject(true);
                obj.put("id", ++citation);
                obj.put("title", title);
                obj.put("content", body);
                obj.put("position", content.getPosition());
                contentArray.add(obj);
            }
            if (!contentArray.isEmpty()) {
                JSONObject group = new JSONObject(true);
                group.put("documentName", entry.getKey());
                group.put("contents", contentArray);
                groups.add(group);
            }
        }
        return JSON.toJSONString(groups);
    }

    public String format(List<? extends RagContent> contents, int maxCharNumber) {
        String data = contents.stream().map(this::formatContent).collect(Collectors.joining("\n\n"));
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
