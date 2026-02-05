package com.tarzan.maxkb4j.common.util;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    /**
     * 将分块列表合并为不超过 limit 的段落
     */
    public static List<String> mergeChunksIntoParts(List<String> chunks, int limit) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : chunks) {
            if (current.isEmpty()) {
                current.append(sentence);
            } else if (current.length() + sentence.length() <= limit) {
                current.append(sentence);
            } else {
                paragraphs.add(current.toString());
                current = new StringBuilder(sentence);
            }
        }
        if (!current.isEmpty()) {
            paragraphs.add(current.toString());
        }
        return paragraphs;
    }
}