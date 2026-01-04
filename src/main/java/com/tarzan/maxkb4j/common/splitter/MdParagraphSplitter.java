package com.tarzan.maxkb4j.common.splitter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimple;

import java.util.List;
import java.util.regex.Pattern;

public class MdParagraphSplitter {

    private static final Splitter PARAGRAPH_SPLITTER = Splitter
            .on(Pattern.compile("\n\\s*\n")) // 支持 \n\n、\n \n、\n\t\n 等
            .trimResults()                   // 去除每个段落首尾空白
            .omitEmptyStrings();

    public static List<ParagraphSimple> split(String markdownText) {
        if (markdownText == null || markdownText.isEmpty()) {
            return ImmutableList.of();
        }
        List<String> paragraphs = PARAGRAPH_SPLITTER.splitToList(markdownText);
        return  paragraphs.stream()
                .map(paragraph -> {
                    String[] titleAndContent = paragraph.split("\n", 2);
                    String title = titleAndContent.length > 0 ? titleAndContent[0] : null;
                    String content = titleAndContent.length > 1 ? titleAndContent[1] : null;
                    return ParagraphSimple.builder()
                            .title(title)
                            .content(content)
                            .build();
                })
                .toList();
    }
}
