package com.tarzan.maxkb4j.common.util;

import java.text.BreakIterator;
import java.util.*;

public class SentenceSplitter {

    /**
     * 使用 BreakIterator 按句子分割，并按 limit 合并为段落
     *
     * @param text   输入文本
     * @param limit  每段最大字符数（>0）
     * @param locale 语言区域（如 Locale.ENGLISH, Locale.CHINESE）
     * @return 分段后的列表
     */
    public static List<String> split(String text, int limit, Locale locale) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }
        // 使用 BreakIterator 获取所有句子
        BreakIterator sentenceIter = BreakIterator.getSentenceInstance(locale);
        sentenceIter.setText(text);
        List<String> sentences = new ArrayList<>();
        int start = sentenceIter.first();
        int end = sentenceIter.next();
        while (end != BreakIterator.DONE) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            start = end;
            end = sentenceIter.next();
        }
        // 合并句子为不超过 limit 的段落
        return mergeSentencesIntoParagraphs(sentences, limit);
    }

    /**
     * 重载方法：使用默认 Locale
     */
    public static List<String> split(String text, int limit) {
        return split(text, limit, Locale.getDefault());
    }

    /**
     * 将句子列表合并为不超过 limit 的段落
     */
    private static List<String> mergeSentencesIntoParagraphs(List<String> sentences, int limit) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            // 如果当前段落为空，直接加入
            if (current.isEmpty()) {
                current.append(sentence);
            }
            // 如果加上这个句子会超限，则先保存当前段落
            else if (current.length() + sentence.length() + 1 <= limit) { // +1 for space or newline (optional)
                // 可选：加空格或换行，但中文通常不需要
                // 这里直接拼接（适用于中英文混合）
                current.append(sentence);
            } else {
                // 超限，保存当前段落
                paragraphs.add(current.toString());
                current = new StringBuilder(sentence);
            }
        }
        // 添加最后一段
        if (!current.isEmpty()) {
            paragraphs.add(current.toString());
        }
        return paragraphs;
    }

}