package com.maxkb4j.core.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.maxkb4j.common.domain.RagContent;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 将检索命中的段落内容注入到用户问题中，拼装成带上下文的 Prompt。
 *
 * <p>通过 {@link RagContent} 最小契约与具体业务 VO 解耦，不依赖任何业务模块。
 */
public class RagContentInjector {
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("question:\n{{userMessage}}\nanswer using the following json information:\n{{contents}}\nOutput Requirements:\nIf the knowledge base content contains image URLs, they must be output in the following Markdown format exactly as they are. ! [](image address)");
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
        variables.put("contents", this.formatJson(contents,maxCharNumber));
        return this.promptTemplate.apply(variables);
    }



    public String formatJson1(List<? extends RagContent> contents, int maxCharNumber) {
        List<RagContent> ragContents=new ArrayList<>();
        int charNumber=0;
        for (RagContent content : contents) {
            String text= content.getTitle()+content.getContent()+content.getDocumentName();
            charNumber=charNumber+text.length();
            if (charNumber>maxCharNumber){
                break;
            }
            ragContents.add(content);
        }
        // 获取 RagContent 自身声明的所有字段名
        Set<String> allowedFields = Arrays.stream(RagContent.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        PropertyFilter filter = (object, name, value) -> {
            // 只序列化 RagContent 中声明的字段
            return allowedFields.contains(name);
        };
        return JSON.toJSONString(ragContents, filter, SerializerFeature.PrettyFormat);
    }

    public String formatJson(List<? extends RagContent> contents, int maxCharNumber) {
        // 1. 字符数截断（保持原有逻辑，但避免拼接字符串产生的额外开销）
        List<RagContent> truncated = new ArrayList<>();
        int charCount = 0;
        for (RagContent content : contents) {
            // 直接累加长度，避免 title + content 创建新字符串
            int len = (content.getTitle() == null ? 0 : content.getTitle().length())
                    + (content.getContent() == null ? 0 : content.getContent().length());
            if (charCount + len > maxCharNumber) {
                break;
            }
            charCount += len;
            truncated.add(content);
        }

        // 2. 分组 + 组内排序 + 构建JSON 一体化处理
        JSONArray result = truncated.stream()
                .collect(Collectors.groupingBy(
                        c -> Optional.ofNullable(c.getDocumentName()).orElse(""),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    // 组内按 position 升序，null 排最后
                    JSONArray contentArray = entry.getValue().stream()
                            .sorted(Comparator.comparing(
                                    RagContent::getPosition,
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            ))
                            .map(e -> {
                                JSONObject obj = new JSONObject();
                                obj.put("title", e.getTitle());
                                obj.put("content", e.getContent());
                                obj.put("position", e.getPosition());
                                return obj;
                            })
                            .collect(Collectors.toCollection(JSONArray::new)); // 直接收集为JSONArray
                    JSONObject group = new JSONObject();
                    group.put("documentName", entry.getKey());
                    group.put("contents", contentArray);
                    return group;
                })
                .collect(Collectors.toCollection(JSONArray::new));

        return JSON.toJSONString(result, SerializerFeature.PrettyFormat);
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
