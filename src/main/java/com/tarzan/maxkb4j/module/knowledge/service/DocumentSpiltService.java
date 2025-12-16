package com.tarzan.maxkb4j.module.knowledge.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class DocumentSpiltService {

    private final DocumentSplitter defaultSplitter = new DocumentByParagraphSplitter(512, 64);

    public List<TextSegment> defaultSplit(String text) {
        Document document = Document.document(text);
        return defaultSplitter.split(document);
    }


    public List<TextSegment> split(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        Document document=Document.document(docText);
        if (patterns != null&&patterns.length > 0) {
            List<TextSegment> textSegments = recursive(document, patterns, limit);
            if (withFilter) {
                textSegments = textSegments.stream()
                        .filter(e -> StringUtils.isNotBlank(e.text()))
                        .filter(distinctByKey(TextSegment::text))
                        .collect(Collectors.toList());
            }
            return textSegments;
        } else {
            return defaultSplitter.split(document);
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


    public List<TextSegment> recursive(List<TextSegment> segments, String pattern) {
        List<TextSegment> result = new ArrayList<>();
        for (TextSegment segment : segments) {
            String text = segment.text();
            if (StringUtils.isNotBlank(text)) {
                String[] split = text.split(pattern);
                for (String s : split) {
                    result.add(TextSegment.textSegment(s));
                }
            }
        }
        return result;
    }


    public List<TextSegment> recursive(Document document, String[] patterns, Integer limit) {
        List<TextSegment> textSegments = new ArrayList<>();
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            if (i == 0) {
                DocumentSplitter splitter = new DocumentByRegexSplitter(pattern, "", 1, 0, new DocumentByCharacterSplitter(limit, 0));
                textSegments = recursive(splitter.split(document), pattern);
            } else {
                textSegments = recursive(textSegments, pattern);
            }
        }
        return textSegments;
    }

}
