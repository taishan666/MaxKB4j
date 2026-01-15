package com.tarzan.maxkb4j.common.util;

import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentenceSplitter {

    // 匹配 Markdown 图片: ![alt](url) 或 ![alt](url "title")
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\([^)]+\\)");

    /**
     * 使用 BreakIterator 按句子分割，并按 limit 合并为段落，同时忽略 Markdown 图片中的 !
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

        // Step 1: 提取并替换所有 Markdown 图片为占位符
        Map<String, String> placeholderToImage = new LinkedHashMap<>();
        String processedText = replaceImagesWithPlaceholders(text, placeholderToImage);

        // Step 2: 使用 BreakIterator 分割句子
        BreakIterator sentenceIter = BreakIterator.getSentenceInstance(locale);
        sentenceIter.setText(processedText);
        List<String> sentences = new ArrayList<>();
        int start = sentenceIter.first();
        int end = sentenceIter.next();
        while (end != BreakIterator.DONE) {
            String sentence = processedText.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            start = end;
            end = sentenceIter.next();
        }

        // Step 3: 合并为段落
        List<String> paragraphs = mergeSentencesIntoParagraphs(sentences, limit);

        // Step 4: 还原占位符为原始图片
        return restoreImagesFromPlaceholders(paragraphs, placeholderToImage);
    }

    /**
     * 重载方法：使用默认 Locale
     */
    public static List<String> split(String text, int limit) {
        return split(text, limit, Locale.getDefault());
    }

    /**
     * 将 Markdown 图片替换为唯一占位符，并记录映射
     */
    private static String replaceImagesWithPlaceholders(String text, Map<String, String> placeholderToImage) {
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (matcher.find()) {
            String image = matcher.group();
            String placeholder = "{{IMG_" + (index++) + "}}";
            placeholderToImage.put(placeholder, image);
            matcher.appendReplacement(sb, placeholder);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将段落中的占位符还原为原始 Markdown 图片
     */
    private static List<String> restoreImagesFromPlaceholders(List<String> paragraphs, Map<String, String> placeholderToImage) {
        List<String> restored = new ArrayList<>();
        for (String para : paragraphs) {
            String restoredPara = para;
            for (Map.Entry<String, String> entry : placeholderToImage.entrySet()) {
                restoredPara = restoredPara.replace(entry.getKey(), entry.getValue());
            }
            restored.add(restoredPara);
        }
        return restored;
    }

    /**
     * 将句子列表合并为不超过 limit 的段落
     */
    private static List<String> mergeSentencesIntoParagraphs(List<String> sentences, int limit) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
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