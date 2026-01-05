package com.tarzan.maxkb4j.module.knowledge.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentSpiltService {

    private final DocumentSplitter defaultSplitter = new DocumentBySentenceSplitter(512, 0);

    public List<TextSegment> defaultSplit(String text) {
        Document document = Document.document(text);
        return defaultSplitter.split(document);
    }


    public List<TextSegment> split(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        Document document=Document.document(docText);
        if (patterns != null&&patterns.length > 0) {
            List<TextSegment> textSegments = recursive(document, patterns, limit);
            if (Boolean.TRUE.equals(withFilter)) {
                textSegments = textSegments.stream()
                        .filter(e -> StringUtils.isNotBlank(e.text()))
                        .collect(Collectors.toList());
            }
            return textSegments;
        } else {
            return defaultSplitter.split(document);
        }
    }


    public List<TextSegment> recursive(Document document, String[] patterns, Integer limit) {
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
