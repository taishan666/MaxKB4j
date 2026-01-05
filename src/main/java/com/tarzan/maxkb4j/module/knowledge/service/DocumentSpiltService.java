package com.tarzan.maxkb4j.module.knowledge.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class DocumentSpiltService {

    private final DocumentSplitter defaultSplitter = new DocumentBySentenceSplitter(512, 0);

    public List<TextSegment> defaultSplit(String text) {
        Document document = Document.document(text);
        return defaultSplitter.split(document);
    }


    public List<TextSegment> split(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        if (patterns != null&&patterns.length > 0) {
            return recursive(docText, patterns, limit,withFilter);
        } else {
            Document document=Document.document(docText);
            return defaultSplitter.split(document);
        }
    }


    // 预编译正则表达式以提高性能
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" +");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{2,}");

    /**
     * 清理字符串中的多余空格和空行：
     * - 多个连续空格 → 一个空格
     * - 多个连续空行（>=2 个 \n） → 保留一个空行（即两个 \n）
     *
     * @param input 原始字符串
     * @return 清理后的字符串
     */
    public static String cleanWhitespace(String input) {
        if (input == null) {
            return null;
        }
        // 将多个连续空格替换为一个空格
        String result = MULTIPLE_SPACES.matcher(input).replaceAll(" ");
        // 将两个或更多连续换行符替换为两个换行符（即保留一个空行）
        result = MULTIPLE_NEWLINES.matcher(result).replaceAll("\n\n");
        // 可选：去除首尾空白（包括换行）
         result = result.trim();
        return result;
    }

    public List<TextSegment> recursive(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        if (Boolean.TRUE.equals(withFilter)) {
            docText=cleanWhitespace(docText);
        }
        Document document=Document.document(docText);
        if (patterns == null || patterns.length == 0) {
            // 如果没有模式，直接返回整个文档作为一个段（或根据需求调整）
            return List.of(); // 假设 TextSegment 构造方式
        }
        // 从最内层开始构建 splitter 链
        DocumentSplitter currentSplitter = new DocumentByWordSplitter(limit,0);
        // 逆序遍历 patterns：从最后一个到第一个
        for (int i = patterns.length - 1; i >= 0; i--) {
            String pattern = patterns[i];
            currentSplitter = new DocumentByRegexSplitter(pattern, "", limit, 0, currentSplitter);
        }
        // 执行分割
        List<TextSegment> result = currentSplitter.split(document);
        return result != null ? result : List.of();
    }
}
