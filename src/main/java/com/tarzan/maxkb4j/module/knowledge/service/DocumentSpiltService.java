package com.tarzan.maxkb4j.module.knowledge.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentSpiltService {

    private final DocumentSplitter defaultSplitter = new DocumentBySentenceSplitter(512, 0);

    public List<TextSegment> defaultSplit(String text) {
        Document document = Document.document(text);
        return defaultSplitter.split(document);
    }


    public List<String> split(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        if (patterns != null&&patterns.length > 0) {
            return recursive(docText, patterns, limit,withFilter);
        } else {
            Document document=Document.document(docText);
            List<TextSegment> textSegments =  defaultSplitter.split(document);
            return textSegments.stream().map(TextSegment::text).toList();
        }
    }


    // 预编译正则表达式以提高性能
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" +");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{2,}");
    // 统一移除 Markdown 标题前缀（支持 1~6 个 # 后跟空格）
    private static final Pattern MARKDOWN_HEADER = Pattern.compile("^#{1,6} ");

    /**
     * 清理字符串中的多余空格和空行，并移除 Markdown 标题符号
     *
     * @param input 原始字符串
     * @return 清理后的字符串
     */
    public static String cleanWhitespace(String input) {
        if (input == null) {
            return null;
        }
        String result = MULTIPLE_SPACES.matcher(input).replaceAll(" ");
        result = MULTIPLE_NEWLINES.matcher(result).replaceAll("\n\n");
        // 使用正则统一移除 Markdown 标题前缀（更健壮）
        result = MARKDOWN_HEADER.matcher(result).replaceAll("");
        return result.trim();
    }



    public List<String> recursive(String docText, String[] patterns, Integer limit, Boolean withFilter) {

        if (docText == null || docText.isEmpty()) {
            return Collections.emptyList();
        }
        // 初始只有一个完整文本
        List<String> parts = Collections.singletonList(docText);
        // 按照 patterns 顺序逐层分割（不考虑 limit）
        for (String regex : patterns) {
            if (regex == null || regex.isEmpty()) {
                continue;
            }
            List<String> newParts = new ArrayList<>();
            for (String part : parts) {
               // 使用带 MULTILINE 和 DOTALL 的模式
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(part);
                int lastEnd = 0;
                while (matcher.find()) {
                    // 输出上一段内容（从上次结束到当前标题前）
                    String contentBefore = part.substring(lastEnd, matcher.start()).trim();
                    if (!contentBefore.isEmpty()) {
                        System.out.println("前一段内容: " + contentBefore);
                        newParts.add(contentBefore);
                    }
                    lastEnd = matcher.start();
                }
                // 最后一段内容
                String finalContent = part.substring(lastEnd).trim();
                if (!finalContent.isEmpty()) {
                    System.out.println("最后一段内容: " + finalContent);
                    newParts.add(finalContent);
                }
            }
            parts = newParts;
        }
        // 所有 pattern 分割完成后，再处理超长片段：按 limit 切分
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                if (part.length() <= limit) {
                    result.add(part);
                } else {
                    // 按字符长度切分，每段最多 limit 个字符
                    int start = 0;
                    int len = part.length();
                    while (start < len) {
                        int end = Math.min(start + limit, len);
                        result.add(part.substring(start, end));
                        start = end;
                    }
                }
            }
        }
        // 应用清理（注意：必须收集返回值）
        if (Boolean.TRUE.equals(withFilter)) {
            return result.stream()
                    .map(DocumentSpiltService::cleanWhitespace)
                    .filter(StringUtils::isNotBlank)
                    .toList(); // Java 16+；若用旧版改用 collect(Collectors.toList())
        }

        return result.stream()
                .filter(StringUtils::isNotBlank)
                .toList();
    }

}
