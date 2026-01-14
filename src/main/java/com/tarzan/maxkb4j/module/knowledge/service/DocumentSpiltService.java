package com.tarzan.maxkb4j.module.knowledge.service;

import com.tarzan.maxkb4j.common.util.SentenceSplitter;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimple;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentSpiltService {

    private final String[] DEFAULT_PATTERNS = {
            "(?<=^)# .*|(?<=\\n)# .*",
            "(?<=\\n)(?<!#)## (?!#).*|(?<=^)(?<!#)## (?!#).*",
            "(?<=\\n)(?<!#)### (?!#).*|(?<=^)(?<!#)### (?!#).*",
            "(?<=\\n)(?<!#)#### (?!#).*|(?<=^)(?<!#)#### (?!#).*",
            "(?<=\\n)(?<!#)##### (?!#).*|(?<=^)(?<!#)##### (?!#).*",
            "(?<=\\n)(?<!#)###### (?!#).*|(?<=^)(?<!#)###### (?!#).*"
    };

    public List<ParagraphSimple> smartSplit(String text) {
        return recursive(text, DEFAULT_PATTERNS, 512, true);
    }


    public List<ParagraphSimple> split(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        if (patterns != null && patterns.length > 0) {
            return recursive(docText, patterns, limit, withFilter);
        } else {
            return smartSplit(docText);
        }
    }


    // 预编译正则表达式以提高性能
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" +");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{2,}");
    // 统一移除 Markdown 标题前缀（支持 1~6 个 # 后跟空格）
    private static final Pattern MARKDOWN_HEADER = Pattern.compile("#{1,6} ");

    /**
     * 清理字符串中的多余空格和空行，并移除 Markdown 标题符号
     *
     * @param input 原始字符串
     * @return 清理后的字符串
     */
    public static String cleanAndFilter(String input) {
        if (StringUtils.isEmpty(input)) {
            return "";
        }
        String result = MULTIPLE_SPACES.matcher(input).replaceAll(" ");
        result = MULTIPLE_NEWLINES.matcher(result).replaceAll("\n");
        // 使用正则统一移除 Markdown 标题前缀（更健壮）
        result = MARKDOWN_HEADER.matcher(result).replaceAll("");
        return result.trim();
    }

    private static String cleanTitle(String input) {
        // 使用正则统一移除 Markdown 标题前缀（更健壮）
        String result = MARKDOWN_HEADER.matcher(input).replaceAll("");
        return result.trim();
    }


    public List<ParagraphSimple> recursive(String docText, String[] patterns, int limit, Boolean withFilter) {
        if (docText == null || docText.isEmpty()) {
            return Collections.emptyList();
        }
        // 初始只有一个完整文本
        List<ParagraphSimple> parts = Collections.singletonList(ParagraphSimple.builder().title("").content(docText).build());
        for (String regex : patterns) {
            if (regex == null || regex.isEmpty()) {
                continue;
            }
            List<ParagraphSimple> newParts = new ArrayList<>();
            for (ParagraphSimple part : parts) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(part.getContent());
                int lastEnd = 0;
                String lastTitle = part.getTitle();
                while (matcher.find()) {
                    // 输出上一段内容（从上次结束到当前标题前）
                    String lastContent = part.getContent().substring(lastEnd, matcher.start()).trim();
                    newParts.add(ParagraphSimple.builder().title(lastTitle).content(lastContent).build());
                    lastTitle = part.getTitle() + " " + cleanTitle(matcher.group());
                    lastEnd = matcher.end();
                }
                // 最后一段内容
                String endContent = part.getContent().substring(lastEnd).trim();
                if (!endContent.isEmpty()) {
                    newParts.add(ParagraphSimple.builder().title(lastTitle).content(endContent).build());
                }
            }
            parts = newParts;
        }
        // 所有 pattern 分割完成后，再处理超长片段：按 limit 切分
        List<ParagraphSimple> result = new ArrayList<>();
        for (ParagraphSimple part : parts) {
            if (StringUtils.isNotBlank(part.getContent())) {
                List<String> texts = SentenceSplitter.split(part.getContent(),limit);
                for (String test : texts) {
                    ParagraphSimple newPart = ParagraphSimple.builder().title(part.getTitle()).content(test).build();
                    result.add(newPart);
                }
            }
        }
        if (Boolean.TRUE.equals(withFilter)) {
            return result.stream()
                    .filter(e -> StringUtils.isNotBlank(e.getContent()))
                    .peek(e -> e.setContent(cleanAndFilter(e.getContent())))
                    .toList();
        }
        return result.stream()
                .filter(e -> StringUtils.isNotBlank(e.getContent()))
                .toList();
    }

}
